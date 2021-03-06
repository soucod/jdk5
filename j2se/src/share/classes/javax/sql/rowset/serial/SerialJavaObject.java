/*
 * @(#)SerialJavaObject.java	1.6 04/05/29
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.sql.rowset.serial;

import java.sql.*;
import java.io.*;
import java.util.Map;
import java.lang.reflect.*;
import javax.sql.rowset.RowSetWarning;

/**
 * A serializable mapping in the Java programming language of an SQL 
 * <code>JAVA_OBJECT</code> value. Assuming the Java object 
 * implements the <code>Serializable</code> interface, this class simply wraps the 
 * serialization process.
 * <P>
 * If however, the serialization is not possible because
 * the Java object is not immediately serializable, this class will
 * attempt to serialize all non-static members to permit the object
 * state to be serialized. 
 * Static or transient fields cannot be serialized; an attempt to serialize
 * them will result in a <code>SerialException</code> object being thrown.
 *
 * @version 0.1
 * @author Jonathan Bruce
 */
public class SerialJavaObject implements Serializable, Cloneable {

    /**
     * Placeholder for object to be serialized.
     */
    private Object obj;


   /**
    * Placeholder for all fields in the <code>JavaObject</code> being serialized.
    */
    private transient Field[] fields;

    /**
     * Constructor for <code>SerialJavaObject</code> helper class.
     * <p>
     * 
     * @param obj the Java <code>Object</code> to be serialized
     * @throws SerialException if the object is found
     * to be unserializable
     */
    public SerialJavaObject(Object obj) throws SerialException {

	// if any static fields are found, an exception
        // should be thrown


	// get Class. Object instance should always be available
	Class c = obj.getClass(); 	

	// determine if object implements Serializable i/f
	boolean serializableImpl = false;
	Class[] theIf = c.getInterfaces();
	for (int i = 0; i < theIf.length; i++) {
	    String ifName = theIf[i].getName();
	    if (ifName == "java.io.Serializable") {
		serializableImpl = true;	
	    }
 	}

	// can only determine public fields (obviously). If
	// any of these are static, this should invalidate
   	// the action of attempting to persist these fields
	// in a serialized form

 	boolean anyStaticFields = false;
	fields = c.getFields();
        //fields = new Object[field.length];

	for (int i = 0; i < fields.length; i++ ) {                 
	    if ( fields[i].getModifiers() == Modifier.STATIC ) {
		anyStaticFields = true;
	    }
            //fields[i] = field[i].get(obj);
	}
        try {
            if (!(serializableImpl)) {
               throw new RowSetWarning("Test");
            }
        } catch (RowSetWarning w) {
            setWarning(w);
        }
        
	if (anyStaticFields) {
	    throw new SerialException("Located static fields in " +
		"object instance. Cannot serialize");
	}

	this.obj = obj;
    }

    /**
     * Returns an <code>Object</code> that is a copy of this <code>SerialJavaObject</code> 
     * object. 
     *
     * @return a copy of this <code>SerialJavaObject</code> object as an
     *         <code>Object</code> in the Java programming language
     * @throws SerialException if the instance is corrupt
     */
    public Object getObject() throws SerialException {
        return this.obj;
    }

    /**
     * Returns an array of <code>Field</code> objects that contains each
     * field of the object that this helper class is serializing.
     * 
     * @return an array of <code>Field</code> objects
     * @throws SerialException if an error is encountered accessing
     * the serialized object 
     */
    public Field[] getFields() throws SerialException {
	if (fields != null) {	
            Class c = this.obj.getClass();
	    return c.getFields();
	} else {
	    throw new SerialException("SerialJavaObject does not contain" +
		" a serialized object instance");
	}
    }
    
    /**
	 * The identifier that assists in the serialization of this 
     * <code>SerialJavaObject</code> object.
     */
    static final long serialVersionUID = -1465795139032831023L;
    
    /**
     * A container for the warnings issued on this <code>SerialJavaObject</code>
     * object. When there are multiple warnings, each warning is chained to the
     * previous warning.
     */
    java.util.Vector chain;
    
    /**
     * Registers the given warning.
     */
    private void setWarning(RowSetWarning e) {
        if (chain == null) {
            chain = new java.util.Vector();
        }
        chain.add(e);
    }
}
