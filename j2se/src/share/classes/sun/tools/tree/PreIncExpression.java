/*
 * @(#)PreIncExpression.java	1.21 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.tools.tree;

import sun.tools.java.*;
import sun.tools.asm.Assembler;

/**
 * WARNING: The contents of this source file are not part of any
 * supported API.  Code that depends on them does so at its own risk:
 * they are subject to change or removal without notice.
 */
public
class PreIncExpression extends IncDecExpression {
    /**
     * Constructor
     */
    public PreIncExpression(long where, Expression right) {
	super(PREINC, where, right);
    }

    /**
     * Code
     */
    public void codeValue(Environment env, Context ctx, Assembler asm) {
	codeIncDec(env, ctx, asm, true, true, true);
    }
    public void code(Environment env, Context ctx, Assembler asm) {
	codeIncDec(env, ctx, asm, true, true, false);
    }
}
