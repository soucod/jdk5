/*
 * @(#)SunDisplayChanger.java	1.9 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt;

import java.awt.Component;
import java.awt.peer.ComponentPeer;
import java.awt.Container;
import java.awt.IllegalComponentStateException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.WeakHashMap;
import sun.awt.DebugHelper;

/**
 * This class is used to aid in keeping track of DisplayChangedListeners and
 * notifying them when a display change has taken place. DisplayChangedListeners
 * are notified when the display's bit depth is changed, or when a top-level
 * window has been dragged onto another screen.
 *
 * It is safe for a DisplayChangedListener to be added while the list is being
 * iterated.
 * 
 * The displayChanged() call is propagated after some occurrence (either
 * due to user action or some other application) causes the display mode
 * (e.g., depth or resolution) to change.  All heavyweight components need
 * to know when this happens because they need to create new surfaceData
 * objects based on the new depth.
 *
 * displayChanged() is also called on Windows when they are moved from one
 * screen to another on a system equipped with multiple displays.
 */
public class SunDisplayChanger {
    private static final DebugHelper dbg = DebugHelper.create(SunDisplayChanger.class);

    // Create a new synchronizedMap with initial capacity of one listener.  
    // It is asserted that the most common case is to have one GraphicsDevice 
    // and one top-level Window.  
    private Map listeners = Collections.synchronizedMap(new WeakHashMap(1));

    public SunDisplayChanger() {}

    /*
     * Add a DisplayChangeListener to this SunDisplayChanger so that it is 
     * notified when the display is changed.
     */
    public void add(DisplayChangedListener theListener) {
        if (dbg.on) {
            dbg.assertion(theListener != null);
        }

        listeners.put(theListener, null);
    }

    /*
     * Remove the given DisplayChangeListener from this SunDisplayChanger.
     */
    public void remove(DisplayChangedListener theListener) {
        if (dbg.on) {
            dbg.assertion(theListener != null);
        }

        listeners.remove(theListener);
    }

    /*
     * Notify our list of DisplayChangedListeners that a display change has
     * taken place by calling their displayChanged() methods.
     */
    public void notifyListeners() {
    // This method is implemented by making a clone of the set of listeners,
    // and then iterating over the clone.  This is because during the course
    // of responding to a display change, it may be appropriate for a 
    // DisplayChangedListener to add or remove itself from a SunDisplayChanger.
    // If the set itself were iterated over, rather than a clone, it is 
    // trivial to get a ConcurrentModificationException by having a
    // DisplayChangedListener remove itself from its list.
    // Because all display change handling is done on the event thread, 
    // synchronization provides no protection against modifying the listener
    // list while in the middle of iterating over it.  -bchristi 7/10/2001

        HashMap listClone;
        Set cloneSet;

        synchronized(listeners) {
            listClone = new HashMap(listeners);
        }

        cloneSet = listClone.keySet();
        Iterator itr = cloneSet.iterator();
        while (itr.hasNext()) {
            DisplayChangedListener current =
             (DisplayChangedListener) itr.next();
            try {
                current.displayChanged();
            } catch (IllegalComponentStateException e) {
                // This DisplayChangeListener is no longer valid.  Most
                // likely, a top-level window was dispose()d, but its
                // Java objects have not yet been garbage collected.  In any
                // case, we no longer need to track this listener, though we
                // do need to remove it from the original list, not the clone.
                listeners.remove(current);
            }
        }
    }

    /*
     * Notify our list of DisplayChangedListeners that a palette change has
     * taken place by calling their paletteChanged() methods.
     */
    public void notifyPaletteChanged() {
    // This method is implemented by making a clone of the set of listeners,
    // and then iterating over the clone.  This is because during the course
    // of responding to a display change, it may be appropriate for a 
    // DisplayChangedListener to add or remove itself from a SunDisplayChanger.
    // If the set itself were iterated over, rather than a clone, it is 
    // trivial to get a ConcurrentModificationException by having a
    // DisplayChangedListener remove itself from its list.
    // Because all display change handling is done on the event thread, 
    // synchronization provides no protection against modifying the listener
    // list while in the middle of iterating over it.  -bchristi 7/10/2001

        HashMap listClone;
        Set cloneSet;

        synchronized (listeners) {
            listClone = new HashMap(listeners);
        }
        cloneSet = listClone.keySet();
        Iterator itr = cloneSet.iterator();
        while (itr.hasNext()) {
            DisplayChangedListener current = 
             (DisplayChangedListener) itr.next();
            try {
                current.paletteChanged();
            } catch (IllegalComponentStateException e) {
                // This DisplayChangeListener is no longer valid.  Most
                // likely, a top-level window was dispose()d, but its
                // Java objects have not yet been garbage collected.  In any
                // case, we no longer need to track this listener, though we
                // do need to remove it from the original list, not the clone.
                listeners.remove(current);
            }
        }
    }
}

