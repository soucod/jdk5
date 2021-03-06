/*
 * @(#)UnsignedAccessViolationException.java	1.9 03/12/19
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.javaws.exceptions;
import java.net.URL;
import com.sun.deploy.resources.ResourceManager;
import com.sun.javaws.jnl.LaunchDesc;

public class UnsignedAccessViolationException extends JNLPException {

    URL _url;
    boolean _initial;
    
    public UnsignedAccessViolationException(LaunchDesc ld, URL url, boolean initial) {
        super(ResourceManager.getString("launch.error.category.security"), ld);
	_url = url;
	_initial = initial;
    }
    
    /** Returns message */
    public String getRealMessage() {
        return (ResourceManager.getString("launch.error.unsignedAccessViolation") + 
		"\n" +ResourceManager.getString("launch.error.unsignedResource",_url.toString()));
    }

    /** 
     *  can override brief message 
     */
     public String getBriefMessage() {
	if (_initial) {
	    return null;
	}
	return ResourceManager.getString("launcherrordialog.brief.continue");
    }
}


