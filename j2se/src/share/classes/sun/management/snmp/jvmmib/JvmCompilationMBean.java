/*
 * @(#)JvmCompilationMBean.java	1.6 04/07/26
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.management.snmp.jvmmib;

//
// Generated by mibgen version 5.0 (06/02/03) when compiling JVM-MANAGEMENT-MIB in standard metadata mode.
//


// jmx imports
//
import com.sun.jmx.snmp.SnmpStatusException;

/**
 * This interface is used for representing the remote management interface for the "JvmCompilation" MBean.
 */
public interface JvmCompilationMBean {

    /**
     * Getter for the "JvmJITCompilerTimeMonitoring" variable.
     */
    public EnumJvmJITCompilerTimeMonitoring getJvmJITCompilerTimeMonitoring() throws SnmpStatusException;

    /**
     * Getter for the "JvmJITCompilerTimeMs" variable.
     */
    public Long getJvmJITCompilerTimeMs() throws SnmpStatusException;

    /**
     * Getter for the "JvmJITCompilerName" variable.
     */
    public String getJvmJITCompilerName() throws SnmpStatusException;

}
