/*
 * @(#)CharType.java	1.11 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.jdi;

/**
 * The type of all primitive char values accessed in
 * the target VM. Calls to {@link Value#type} will return an 
 * implementor of this interface.
 *
 * @see CharValue
 *
 * @author James McIlree
 * @since  1.3
 */
public interface CharType extends PrimitiveType
{
}

