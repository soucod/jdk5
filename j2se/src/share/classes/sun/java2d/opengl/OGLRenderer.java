/*
 * @(#)OGLRenderer.java	1.5 04/02/17
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.java2d.opengl;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.GradientPaint;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.IllegalPathStateException;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import sun.awt.image.PixelConverter;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.pipe.LoopPipe;
import sun.java2d.pipe.PixelDrawPipe;
import sun.java2d.pipe.PixelFillPipe;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.ShapeDrawPipe;
import sun.java2d.pipe.SpanIterator;
import sun.java2d.pipe.ShapeSpanIterator;

public abstract class OGLRenderer implements
    PixelDrawPipe,
    PixelFillPipe,
    ShapeDrawPipe
{
    abstract long getContext(SunGraphics2D sg2d);
    abstract void releaseContext(long pCtx);

    native void enableGradientPaint(long pCtx,
                                    boolean cyclic,
                                    double p0, double p1, double p3,
                                    int pixel1, int pixel2);
    native void disableGradientPaint(long pCtx);
    native void enableTexturePaint(long pCtx, long pSrcOps, boolean filter,
                                   double xp0, double xp1, double xp3,
                                   double yp0, double yp1, double yp3);
    native void disableTexturePaint(long pCtx);

    protected long getContext(SunGraphics2D sg2d, int pixel, boolean opaque) {
        int ctxflags;
        if (opaque) {
            ctxflags = OGLContext.SRC_IS_PREMULT | OGLContext.SRC_IS_OPAQUE;
        } else {
            ctxflags = OGLContext.SRC_IS_PREMULT;
        }

        OGLSurfaceData dstData = (OGLSurfaceData)sg2d.surfaceData;
        return OGLContext.getContext(dstData, dstData,
                                     sg2d.getCompClip(), sg2d.composite,
                                     null /*sg2d.transform*/, pixel,
                                     ctxflags);
    }

    public static class Solid extends OGLRenderer {
        protected long getContext(SunGraphics2D sg2d) {
            int pixel = sg2d.pixel;
            return getContext(sg2d, pixel, pixel >>> 24 == 0xff);
        }

        protected void releaseContext(long pCtx) {
        }
    }

    public static class Gradient extends OGLRenderer {

        /**
         * We use OpenGL's texture coordinate generator to automatically
         * apply a smooth gradient (either cyclic or acyclic) to the geometry
         * being rendered.  This technique is almost identical to the one
         * described in the comments for OGLRenderer.Texture.getContext(),
         * except the calculations take place in one dimension instead of two.
         * Instead of an anchor rectangle in the TexturePaint case, we use
         * the vector between the two GradientPaint end points in our
         * calculations.  The generator uses a single plane equation that
         * takes the (x,y) location (in device space) of the fragment being
         * rendered to calculate a (u) texture coordinate for that fragment:
         *     u = Ax + By + Cz + Dw
         *
         * The gradient renderer uses a two-pixel 1D texture where the first
         * pixel contains the first GradientPaint color, and the second pixel
         * contains the second GradientPaint color.  (Note that we repeat
         * those colors in the texture borders so that acyclic gradients
         * clamp the colors properly at the extremes.)  The following diagram
         * attempts to show the layout of the texture containing the two
         * GradientPaint colors (C1 and C2):
         *
         *                -----------------------------------
         *               |   C1   |   C1   |   C2   |   C2   |
         *               |(border)|        |        |(border)|
         *                -----------------------------------
         *                      u=0  .25  .5   .75  1
         *
         * We calculate our plane equation constants (A,B,D) such that u=0.25
         * corresponds to the first GradientPaint end point in user space and
         * u=0.75 corresponds to the second end point.  This is somewhat
         * non-obvious, but since the gradient colors are generated by
         * interpolating between C1 and C2, we want the pure color at the
         * end points, and we will get the pure color only when u correlates
         * to the center of a texel.  The following chart shows the expected
         * color for some sample values of u (where C' is the color halfway
         * between C1 and C2):
         *
         *       u value      acyclic (GL_CLAMP)      cyclic (GL_REPEAT)
         *       -------      ------------------      ------------------
         *        -0.25              C1                       C2
         *         0.0               C1                       C'
         *         0.25              C1                       C1
         *         0.5               C'                       C'
         *         0.75              C2                       C2
         *         1.0               C2                       C'
         *         1.25              C2                       C1
         *
         * Original inspiration for this technique came from UMD's Agile2D
         * project (GradientManager.java).
         */
        protected long getContext(SunGraphics2D sg2d) {
            GradientPaint paint = (GradientPaint)sg2d.paint;
            boolean opaque = (paint.getTransparency() == Transparency.OPAQUE);
            long pCtx = getContext(sg2d, 0xffffffff, opaque);

            // convert gradient colors to IntRgbaPre format
            PixelConverter pc = PixelConverter.RgbaPre.instance;
            Color c1 = paint.getColor1();
            int pixel1 = pc.rgbToPixel(c1.getRGB(), null);
            Color c2 = paint.getColor2();
            int pixel2 = pc.rgbToPixel(c2.getRGB(), null);

            // calculate plane equation constants
            AffineTransform at = (AffineTransform)sg2d.transform.clone();
            Point2D p = paint.getPoint1();
            double x = p.getX();
            double y = p.getY();
            at.translate(x, y);
            // now gradient point 1 is at the origin
            p = paint.getPoint2();
            x = p.getX() - x;
            y = p.getY() - y;
            double len = Math.sqrt(x * x + y * y);
            at.rotate(Math.atan2(y, x));
            // now gradient point 2 is on the positive x-axis
            at.scale(2*len, 1);
            // now gradient point 2 is at (0.5, 0)
            at.translate(-0.25, 0);
            // now gradient point 1 is at (0.25, 0), point 2 is at (0.75, 0)

            double p0, p1, p3;
            try {
                at = at.createInverse();
                p0 = at.getScaleX();
                p1 = at.getShearX();
                p3 = at.getTranslateX();
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                p0 = p1 = p3 = 0.0;
            }

            enableGradientPaint(pCtx, paint.isCyclic(),
                                p0, p1, p3, pixel1, pixel2);

            return pCtx;
        }

        protected void releaseContext(long pCtx) {
            disableGradientPaint(pCtx);
        }
    }

    public static class Texture extends OGLRenderer {

        /**
         * Returns true if the given TexturePaint instance can be used by the
         * accelerated OGLRenderer.Texture implementation.  A TexturePaint is
         * considered valid if the following conditions are met:
         *   - the texture image dimensions are power-of-two (or the
         *     GL_ARB_texture_non_power_of_two extension is present)
         *   - the texture image can be (or is already) cached in an OpenGL
         *     texture object
         */
        public static boolean isPaintValid(SunGraphics2D sg2d,
                                           TexturePaint paint)
        {
            OGLSurfaceData dstData = (OGLSurfaceData)sg2d.surfaceData;
            BufferedImage bi = paint.getImage();

            // see if texture-non-pow2 extension is available
            if (!dstData.isTexNonPow2Available()) {
                int imgw = bi.getWidth();
                int imgh = bi.getHeight();

                // verify that the texture image dimensions are pow2
                if ((imgw & (imgw - 1)) != 0 || (imgh & (imgh - 1)) != 0) {
                    return false;
                }
            }

            SurfaceData srcData =
                SurfaceData.getSourceSurfaceData(bi, dstData,
                                                 CompositeType.SrcOver,
                                                 null, false);
            if (!(srcData instanceof OGLSurfaceData)) {
                // REMIND: this is a hack that attempts to cache the system
                //         memory image from the TexturePaint instance into an
                //         OpenGL texture...
                srcData =
                    SurfaceData.getSourceSurfaceData(bi, dstData,
                                                     CompositeType.SrcOver,
                                                     null, false);
                if (!(srcData instanceof OGLSurfaceData)) {
                    return false;
                }
            }

            // verify that the source surface is actually a texture
            OGLSurfaceData oglData = (OGLSurfaceData)srcData;
            if (oglData.getType() != OGLSurfaceData.TEXTURE) {
                return false;
            }

            return true;
        }

        /**
         * We use OpenGL's texture coordinate generator to automatically
         * map the TexturePaint image to the geometry being rendered.  The
         * generator uses two separate plane equations that take the (x,y)
         * location (in device space) of the fragment being rendered to
         * calculate (u,v) texture coordinates for that fragment:
         *     u = Ax + By + Cz + Dw
         *     v = Ex + Fy + Gz + Hw
         *
         * Since we use a 2D orthographic projection, we can assume that z=0
         * and w=1 for any fragment.  So we need to calculate appropriate
         * values for the plane equation constants (A,B,D) and (E,F,H) such
         * that {u,v}=0 for the top-left of the TexturePaint's anchor
         * rectangle and {u,v}=1 for the bottom-right of the anchor rectangle.
         * We can easily make the texture image repeat for {u,v} values
         * outside the range [0,1] by specifying the GL_REPEAT texture wrap
         * mode.
         *
         * Calculating the plane equation constants is surprisingly simple.
         * We can think of it as an inverse matrix operation that takes
         * device space coordinates and transforms them into user space
         * coordinates that correspond to a location relative to the anchor
         * rectangle.  First, we translate and scale the current user space
         * transform by applying the anchor rectangle bounds.  We then take
         * the inverse of this affine transform.  The rows of the resulting
         * inverse matrix correlate nicely to the plane equation constants
         * we were seeking.
         */
        protected long getContext(SunGraphics2D sg2d) {
            TexturePaint paint = (TexturePaint)sg2d.paint;
            boolean opaque = (paint.getTransparency() == Transparency.OPAQUE);
            long pCtx = getContext(sg2d, 0xffffffff, opaque);

            BufferedImage bi = paint.getImage();
            SurfaceData dstData = sg2d.surfaceData;
            SurfaceData srcData =
                SurfaceData.getSourceSurfaceData(bi, dstData,
                                                 CompositeType.SrcOver,
                                                 null, false);
            if (!(srcData instanceof OGLSurfaceData)) {
                // REMIND: return error value?
                return pCtx;
            }

            boolean filter =
                (sg2d.interpolationType !=
                 AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

            // calculate plane equation constants
            AffineTransform at = (AffineTransform)sg2d.transform.clone();
            Rectangle2D anchor = paint.getAnchorRect();
            at.translate(anchor.getX(), anchor.getY());
            at.scale(anchor.getWidth(), anchor.getHeight());

            double xp0, xp1, xp3, yp0, yp1, yp3;
            try {
                at = at.createInverse();
                xp0 = at.getScaleX();
                xp1 = at.getShearX();
                xp3 = at.getTranslateX();
                yp0 = at.getShearY();
                yp1 = at.getScaleY();
                yp3 = at.getTranslateY();
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                xp0 = xp1 = xp3 = yp0 = yp1 = yp3 = 0.0;
            }

            long pSrcOps = srcData.getNativeOps();
            enableTexturePaint(pCtx, pSrcOps, filter,
                               xp0, xp1, xp3,
                               yp0, yp1, yp3);

            return pCtx;
        }

        protected void releaseContext(long pCtx) {
            disableTexturePaint(pCtx);
        }
    }

    native void doDrawLine(long pCtx, int x1, int y1, int x2, int y2);

    public void drawLine(SunGraphics2D sg2d,
                         int x1, int y1, int x2, int y2)
    {
        synchronized (OGLContext.LOCK) {
            long oglc = getContext(sg2d);
            int transx = sg2d.transX;
            int transy = sg2d.transY;
            doDrawLine(oglc, x1+transx, y1+transy, x2+transx, y2+transy);
            releaseContext(oglc);
        }
    }

    native void doDrawRect(long pCtx, int x, int y, int w, int h);

    public void drawRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        synchronized (OGLContext.LOCK) {
            long oglc = getContext(sg2d);
            doDrawRect(oglc, x+sg2d.transX, y+sg2d.transY, width, height);
            releaseContext(oglc);
        }
    }

    public void drawRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        draw(sg2d, new RoundRectangle2D.Float(x, y, width, height,
                                              arcWidth, arcHeight));
    }

    public void drawOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        draw(sg2d, new Ellipse2D.Float(x, y, width, height));
    }

    public void drawArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        draw(sg2d, new Arc2D.Float(x, y, width, height,
                                   startAngle, arcAngle,
                                   Arc2D.OPEN));
    }

    native void doDrawPoly(long pCtx, int transx, int transy,
                           int[] xpoints, int[] ypoints,
                           int npoints, boolean isclosed);

    public void drawPolyline(SunGraphics2D sg2d,
                             int xpoints[], int ypoints[],
                             int npoints)
    {
        synchronized (OGLContext.LOCK) {
            long oglc = getContext(sg2d);
            doDrawPoly(oglc, sg2d.transX, sg2d.transY,
                       xpoints, ypoints, npoints, false);
            releaseContext(oglc);
        }
    }

    public void drawPolygon(SunGraphics2D sg2d,
                            int xpoints[], int ypoints[],
                            int npoints)
    {
        synchronized (OGLContext.LOCK) {
            long oglc = getContext(sg2d);
            doDrawPoly(oglc, sg2d.transX, sg2d.transY,
                       xpoints, ypoints, npoints, true);
            releaseContext(oglc);
        }
    }

    native void doFillRect(long pCtx, int x, int y, int w, int h);

    public void fillRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        synchronized (OGLContext.LOCK) {
            long oglc = getContext(sg2d);
            doFillRect(oglc, x+sg2d.transX, y+sg2d.transY, width, height);
            releaseContext(oglc);
        }
    }

    public void fillRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        fill(sg2d, new RoundRectangle2D.Float(x, y, width, height,
                                              arcWidth, arcHeight));
    }

    public void fillOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        fill(sg2d, new Ellipse2D.Float(x, y, width, height));
    }

    public void fillArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        fill(sg2d, new Arc2D.Float(x, y, width, height,
                                   startAngle, arcAngle,
                                   Arc2D.PIE));
    }

    public void fillPolygon(SunGraphics2D sg2d,
                            int xpoints[], int ypoints[],
                            int npoints)
    {
        fill(sg2d, new Polygon(xpoints, ypoints, npoints));
    }

    public void draw(SunGraphics2D sg2d, Shape s) {
        if (sg2d.strokeState == sg2d.STROKE_THIN) {
            AffineTransform at;
            Polygon p;
            if (sg2d.transformState < sg2d.TRANSFORM_TRANSLATESCALE) {
                if (s instanceof Polygon) {
                    p = (Polygon) s;
                    drawPolygon(sg2d, p.xpoints, p.ypoints, p.npoints);
                    return;
                }
                at = null;
            } else {
                at = sg2d.transform;
            }
            PathIterator pi = s.getPathIterator(at, 0.5f);
            p = new Polygon();
            float coords[] = new float[2];
            while (!pi.isDone()) {
                switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    if (p.npoints > 1) {
                        drawPolyline(sg2d, p.xpoints, p.ypoints, p.npoints);
                    }
                    p.reset();
                    p.addPoint((int) Math.floor(coords[0]),
                               (int) Math.floor(coords[1]));
                    break;
                case PathIterator.SEG_LINETO:
                    if (p.npoints == 0) {
                        throw new IllegalPathStateException
                            ("missing initial moveto in path definition");
                    }
                    p.addPoint((int) Math.floor(coords[0]),
                               (int) Math.floor(coords[1]));
                    break;
                case PathIterator.SEG_CLOSE:
                    if (p.npoints > 0) {
                        p.addPoint(p.xpoints[0], p.ypoints[0]);
                    }
                    break;
                default:
                    throw new IllegalPathStateException("path not flattened");
                }
                pi.next();
            }
            if (p.npoints > 1) {
                drawPolyline(sg2d, p.xpoints, p.ypoints, p.npoints);
            }
        } else if (sg2d.strokeState < sg2d.STROKE_CUSTOM) {
            ShapeSpanIterator si = LoopPipe.getStrokeSpans(sg2d, s);
            try {
                synchronized (OGLContext.LOCK) {
                    long oglc = getContext(sg2d);
                    devFillSpans(oglc, si, si.getNativeIterator(), 0, 0);
                    releaseContext(oglc);
                }
            } finally {
                si.dispose();
            }
        } else {
            fill(sg2d, sg2d.stroke.createStrokedShape(s));
        }
    }

    native void devFillSpans(long pCtx, SpanIterator si, long iterator,
                             int transx, int transy);

    public void fill(SunGraphics2D sg2d, Shape s) {
        AffineTransform at;
        int transx, transy;

        if (sg2d.transformState < sg2d.TRANSFORM_TRANSLATESCALE) {
            // Transform (translation) will be done by devFillSpans
            at = null;
            transx = sg2d.transX;
            transy = sg2d.transY;
        } else {
            // Transform will be done by the PathIterator
            at = sg2d.transform;
            transx = transy = 0;
        }
        ShapeSpanIterator ssi = new ShapeSpanIterator(sg2d, false);
        try {
            // Subtract transx/y from the SSI clip to match the
            // (potentially untranslated) geometry fed to it
            Region clip = sg2d.getCompClip();
            ssi.setOutputAreaXYXY(clip.getLoX() - transx,
                                  clip.getLoY() - transy,
                                  clip.getHiX() - transx,
                                  clip.getHiY() - transy);
            ssi.appendPath(s.getPathIterator(at));
            synchronized (OGLContext.LOCK) {
                long oglc = getContext(sg2d);
                devFillSpans(oglc, ssi, ssi.getNativeIterator(),
                             transx, transy);
                releaseContext(oglc);
            }
        } finally {
            ssi.dispose();
        }
    }

    native void devCopyArea(long pCtx, long pDst,
                            int srcx, int srcy, int dstx, int dsty,
                            int w, int h);

    public OGLRenderer traceWrap() {
	return new Tracer(this);
    }

    private class Tracer extends OGLRenderer {
        private OGLRenderer oglr;

        public Tracer(OGLRenderer oglr) {
            this.oglr = oglr;
        }

        long getContext(SunGraphics2D sg2d) {
            return oglr.getContext(sg2d);
        }

        void releaseContext(long pCtx) {
            oglr.releaseContext(pCtx);
        }

        void doDrawLine(long pCtx, int x1, int y1, int x2, int y2) {
	    GraphicsPrimitive.tracePrimitive("OGLDrawLine");
	    oglr.doDrawLine(pCtx, x1, y1, x2, y2);
	}
        void doDrawRect(long pCtx, int x, int y, int w, int h) {
	    GraphicsPrimitive.tracePrimitive("OGLDrawRect");
	    oglr.doDrawRect(pCtx, x, y, w, h);
	}
        void doDrawPoly(long pCtx, int transx, int transy,
			int[] xpoints, int[] ypoints,
			int npoints, boolean isclosed)
	{
	    GraphicsPrimitive.tracePrimitive("OGLDrawPoly");
	    oglr.doDrawPoly(pCtx, transx, transy,
                             xpoints, ypoints, npoints, isclosed);
	}
        void doFillRect(long pCtx, int x, int y, int w, int h) {
	    GraphicsPrimitive.tracePrimitive("OGLFillRect");
	    oglr.doFillRect(pCtx, x, y, w, h);
	}
        void devFillSpans(long pCtx, SpanIterator si, long iterator,
                          int transx, int transy)
	{
            GraphicsPrimitive.tracePrimitive("OGLFillSpans");
            oglr.devFillSpans(pCtx, si, iterator, transx, transy);
	}
        void devCopyArea(long pCtx, long pDst,
                         int srcx, int srcy,
                         int dstx, int dsty,
                         int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("OGLCopyArea");
            oglr.devCopyArea(pCtx, pDst, srcx, srcy, dstx, dsty, w, h);
	}
    }
}
