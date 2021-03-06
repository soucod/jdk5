/*
 * @(#)MethodBreakpointSpec.java	1.10 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
/*
 * Copyright (c) 1997-1999 by Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 * 
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package com.sun.tools.example.debug.bdi;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class MethodBreakpointSpec extends BreakpointSpec {
    String methodId;
    List methodArgs;

    MethodBreakpointSpec(EventRequestSpecList specs,  
                         ReferenceTypeSpec refSpec, 
                         String methodId, List methodArgs) {
        super(specs, refSpec);
        this.methodId = methodId;
        this.methodArgs = methodArgs;
    }

    /**
     * The 'refType' is known to match.
     */
    void resolve(ReferenceType refType) throws MalformedMemberNameException,
                                             AmbiguousMethodException,
                                             InvalidTypeException,
                                             NoSuchMethodException,
                                             NoSessionException {
        if (!isValidMethodName(methodId)) {
            throw new MalformedMemberNameException(methodId);
        }
        if (!(refType instanceof ClassType)) {
            throw new InvalidTypeException();
        }
        Location location = location((ClassType)refType);
        setRequest(refType.virtualMachine().eventRequestManager()
                   .createBreakpointRequest(location));
    }

    private Location location(ClassType clazz) throws 
                                               AmbiguousMethodException,
                                               NoSuchMethodException,
                                               NoSessionException {
        Method method = findMatchingMethod(clazz);
        Location location = method.location();
        return location;
    }

    public String methodName() {
        return methodId;
    }

    public List methodArgs() {
        return methodArgs;
    }

    public int hashCode() {
        return refSpec.hashCode() + 
            ((methodId != null) ? methodId.hashCode() : 0) +
            ((methodArgs != null) ? methodArgs.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (obj instanceof MethodBreakpointSpec) {
            MethodBreakpointSpec breakpoint = (MethodBreakpointSpec)obj;

            return methodId.equals(breakpoint.methodId) &&
                   methodArgs.equals(breakpoint.methodArgs) &&
                   refSpec.equals(breakpoint.refSpec);
        } else {
            return false;
        }
    }

    public String errorMessageFor(Exception e) { 
        if (e instanceof AmbiguousMethodException) {
            return ("Method " + methodName() + " is overloaded; specify arguments");
            /*
             * TO DO: list the methods here
             */
        } else if (e instanceof NoSuchMethodException) {
            return ("No method " + methodName() + " in " + refSpec);
        } else if (e instanceof InvalidTypeException) {
            return ("Breakpoints can be located only in classes. " + 
                        refSpec + " is an interface or array");
        } else {
            return super.errorMessageFor( e);
        } 
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("breakpoint ");
        buffer.append(refSpec.toString());
        buffer.append('.');
        buffer.append(methodId);
        if (methodArgs != null) {
            Iterator iter = methodArgs.iterator();
            boolean first = true;
            buffer.append('(');
            while (iter.hasNext()) {
                if (!first) {
                    buffer.append(',');
                }
                buffer.append((String)iter.next());
                first = false;
            }
            buffer.append(")");
        }
        buffer.append(" (");
        buffer.append(getStatusString());
        buffer.append(')');
        return buffer.toString();
    }

    private boolean isValidMethodName(String s) {
        return isJavaIdentifier(s) || 
               s.equals("<init>") ||
               s.equals("<clinit>");
    }

    /* 
     * Compare a method's argument types with a Vector of type names.
     * Return true if each argument type has a name identical to the 
     * corresponding string in the vector (allowing for varargs)
     * and if the number of arguments in the method matches the 
     * number of names passed
     */
    private boolean compareArgTypes(Method method, List nameList) {
        List argTypeNames = method.argumentTypeNames();

        // If argument counts differ, we can stop here
        if (argTypeNames.size() != nameList.size()) {
            return false;
        }

        // Compare each argument type's name
        int nTypes = argTypeNames.size();
        for (int i = 0; i < nTypes; ++i) {
            String comp1 = (String)argTypeNames.get(i);
            String comp2 = (String)nameList.get(i);
            if (! comp1.equals(comp2)) {
                /*
                 * We have to handle varargs.  EG, the
                 * method's last arg type is xxx[]
                 * while the nameList contains xxx...
                 * Note that the nameList can also contain
                 * xxx[] in which case we don't get here.
                 */
                if (i != nTypes - 1 || 
                    !method.isVarArgs()  ||
                    !comp2.endsWith("...")) {
                    return false;
                }
                /*
                 * The last types differ, it is a varargs
                 * method and the nameList item is varargs.
                 * We just have to compare the type names, eg,
                 * make sure we don't have xxx[] for the method
                 * arg type and yyy... for the nameList item.
                 */
                int comp1Length = comp1.length();
                if (comp1Length + 1 != comp2.length()) {
                    // The type names are different lengths
                    return false;
                }
                // We know the two type names are the same length
                if (!comp1.regionMatches(0, comp2, 0, comp1Length - 2)) {
                    return false;
                }
                // We do have xxx[] and xxx... as the last param type
                return true;
            }
        }

        return true;
    }

  private VirtualMachine vm() {
    return request.virtualMachine();
  }

  /**
     * Remove unneeded spaces and expand class names to fully 
     * qualified names, if necessary and possible.
     */
    private String normalizeArgTypeName(String name) throws NoSessionException {
        /* 
         * Separate the type name from any array modifiers, 
         * stripping whitespace after the name ends.
         */
        int i = 0;
        StringBuffer typePart = new StringBuffer();
        StringBuffer arrayPart = new StringBuffer();
        name = name.trim();
        int nameLength = name.length();
        /*
         * For varargs, there can be spaces before the ... but not
         * within the ...  So, we will just ignore the ...
         * while stripping blanks.
         */
        boolean isVarArgs = name.endsWith("...");
        if (isVarArgs) {
            nameLength -= 3;
        }

        while (i < nameLength) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c) || c == '[') {
                break;      // name is complete
            }
            typePart.append(c);
            i++;
        }
        while (i < nameLength) {
            char c = name.charAt(i);
            if ( (c == '[') || (c == ']')) {
                arrayPart.append(c);
            } else if (!Character.isWhitespace(c)) {
                throw new IllegalArgumentException(
                                                "Invalid argument type name");

            }
            i++;
        }

        name = typePart.toString();

        /*
         * When there's no sign of a package name already, 
	 * try to expand the 
         * the name to a fully qualified class name
         */
        if ((name.indexOf('.') == -1) || name.startsWith("*.")) {
            try {
                List refs = specs.runtime.findClassesMatchingPattern(name);
                if (refs.size() > 0) {  //### ambiguity???
                    name = ((ReferenceType)(refs.get(0))).name();
                }
            } catch (IllegalArgumentException e) {
                // We'll try the name as is 
            }
        }
        name += arrayPart.toString();
        if (isVarArgs) {
            name += "...";
        }
        return name;
    }

    /* 
     * Attempt an unambiguous match of the method name and 
     * argument specification to a method. If no arguments 
     * are specified, the method must not be overloaded.
     * Otherwise, the argument types much match exactly 
     */
    private Method findMatchingMethod(ClassType clazz) 
                                        throws AmbiguousMethodException,
                                               NoSuchMethodException,
                                               NoSessionException  {

        // Normalize the argument string once before looping below.
        List argTypeNames = null;
        if (methodArgs() != null) {
            argTypeNames = new ArrayList(methodArgs().size());
            Iterator iter = methodArgs().iterator();
            while (iter.hasNext()) {
                String name = (String)iter.next();
                name = normalizeArgTypeName(name);
                argTypeNames.add(name);
            }
        }

        // Check each method in the class for matches
        Iterator iter = clazz.methods().iterator();
        Method firstMatch = null;  // first method with matching name
        Method exactMatch = null;  // (only) method with same name & sig
        int matchCount = 0;        // > 1 implies overload
        while (iter.hasNext()) {
            Method candidate = (Method)iter.next();

            if (candidate.name().equals(methodName())) {
                matchCount++;

                // Remember the first match in case it is the only one
                if (matchCount == 1) {
                    firstMatch = candidate;
                }

                // If argument types were specified, check against candidate
                if ((argTypeNames != null) 
                        && compareArgTypes(candidate, argTypeNames) == true) {
                    exactMatch = candidate;
                    break;
                }
            }
        }

        // Determine method for breakpoint
        Method method = null;
        if (exactMatch != null) {
            // Name and signature match
            method = exactMatch;
        } else if ((argTypeNames == null) && (matchCount > 0)) {
            // At least one name matched and no arg types were specified
            if (matchCount == 1) {
                method = firstMatch;       // Only one match; safe to use it
            } else {
                throw new AmbiguousMethodException();
            }
        } else {
            throw new NoSuchMethodException(methodName());
        }
        return method;
    }
}
