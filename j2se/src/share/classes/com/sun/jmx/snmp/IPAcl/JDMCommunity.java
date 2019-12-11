/*
 * @(#)file      JDMCommunity.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   4.8
 * @(#)date      04/10/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */


/* Generated By:JJTree: Do not edit this line. JDMCommunity.java */

package com.sun.jmx.snmp.IPAcl;

/** 
 * @version     4.8     12/19/03 
 * @author      Sun Microsystems, Inc. 
 */ 
class JDMCommunity extends SimpleNode {
  protected String communityString= "";

  JDMCommunity(int id) {
    super(id);
  }

  JDMCommunity(Parser p, int id) {
    super(p, id);
  }

  public static Node jjtCreate(int id) {
      return new JDMCommunity(id);
  }

  public static Node jjtCreate(Parser p, int id) {
      return new JDMCommunity(p, id);
  }

  public String getCommunity(){
	return communityString;
  }
}