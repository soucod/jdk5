/* -*- Mode: C; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-*/

/*
 * @(#)protocol.c	1.7 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/* This file is mainly provides string conversions for the protocol codes
 * for debugging purposes. 
 */
#include <protocol.h>
#include <stdio.h>

typedef struct protocol_name_pair_ {
    int protocol;
    char *name;
} protocol_name_pair;

/* Should be updated as new protocol descriptors are added */
#define N_PROTOCOL_DESCRIPTORS 166

protocol_name_pair protocol_as_str[N_PROTOCOL_DESCRIPTORS] = {
    { JAVA_PLUGIN_RETURN ,"JAVA_PLUGIN_RETURN"},
    { JAVA_PLUGIN_REQUEST ,"JAVA_PLUGIN_REQUEST"},
    { JAVA_PLUGIN_JNI_VERSION ,"JAVA_PLUGIN_JNI_VERSION"},
    { JAVA_PLUGIN_DEFINE_CLASS ,"JAVA_PLUGIN_DEFINE_CLASS"},
    { JAVA_PLUGIN_FIND_CLASS ,"JAVA_PLUGIN_FIND_CLASS"},
    { JAVA_PLUGIN_GET_SUPER_CLASS ,"JAVA_PLUGIN_GET_SUPER_CLASS"},
    { JAVA_PLUGIN_IS_SUBCLASS_OF ,"JAVA_PLUGIN_IS_SUBCLASS_OF"},
    { JAVA_PLUGIN_THROW ,"JAVA_PLUGIN_THROW"},
    { JAVA_PLUGIN_THROW_NEW ,"JAVA_PLUGIN_THROW_NEW"},
    { JAVA_PLUGIN_EXCEPTION_OCCURED ,"JAVA_PLUGIN_EXCEPTION_OCCURED"},
    { JAVA_PLUGIN_EXCEPTION_DESCRIBE ,"JAVA_PLUGIN_EXCEPTION_DESCRIBE"},
    { JAVA_PLUGIN_EXCEPTION_CLEAR ,"JAVA_PLUGIN_EXCEPTION_CLEAR"},
    { JAVA_PLUGIN_FATAL_ERROR ,"JAVA_PLUGIN_FATAL_ERROR"},
    { JAVA_PLUGIN_NEW_GLOBAL_REF ,"JAVA_PLUGIN_NEW_GLOBAL_REF"},
    { JAVA_PLUGIN_RELEASE_GLOBAL_REF ,"JAVA_PLUGIN_RELEASE_GLOBAL_REF"},
    { JAVA_PLUGIN_RELEASE_LOCAL_REF ,"JAVA_PLUGIN_RELEASE_LOCAL_REF"},
    { JAVA_PLUGIN_IS_SAME_OBJECT ,"JAVA_PLUGIN_IS_SAME_OBJECT"},
    { JAVA_PLUGIN_ALLOC_OBJECT ,"JAVA_PLUGIN_ALLOC_OBJECT"},
    { JAVA_PLUGIN_NEW_OBJECT_METHOD ,"JAVA_PLUGIN_NEW_OBJECT_METHOD"},
    { JAVA_PLUGIN_GET_OBJECT_CLASS ,"JAVA_PLUGIN_GET_OBJECT_CLASS"},
    { JAVA_PLUGIN_IS_INSTANCE_OF ,"JAVA_PLUGIN_IS_INSTANCE_OF"},
    { JAVA_PLUGIN_GET_METHOD_ID ,"JAVA_PLUGIN_GET_METHOD_ID"},
    { JAVA_PLUGIN_CALL_METHOD ,"JAVA_PLUGIN_CALL_METHOD"},
    { JAVA_PLUGIN_CALL_OBJECT_METHOD ,"JAVA_PLUGIN_CALL_OBJECT_METHOD"},
    { JAVA_PLUGIN_CALL_BOOLEAN_METHOD ,"JAVA_PLUGIN_CALL_BOOLEAN_METHOD"},
    { JAVA_PLUGIN_CALL_BYTE_METHOD ,"JAVA_PLUGIN_CALL_BYTE_METHOD"},
    { JAVA_PLUGIN_CALL_CHAR_METHOD ,"JAVA_PLUGIN_CALL_CHAR_METHOD"},
    { JAVA_PLUGIN_CALL_SHORT_METHOD ,"JAVA_PLUGIN_CALL_SHORT_METHOD"},
    { JAVA_PLUGIN_CALL_INT_METHOD ,"JAVA_PLUGIN_CALL_INT_METHOD"},
    { JAVA_PLUGIN_CALL_LONG_METHOD ,"JAVA_PLUGIN_CALL_LONG_METHOD"},
    { JAVA_PLUGIN_CALL_FLOAT_METHOD ,"JAVA_PLUGIN_CALL_FLOAT_METHOD"},
    { JAVA_PLUGIN_CALL_DOUBLE_METHOD ,"JAVA_PLUGIN_CALL_DOUBLE_METHOD"},
    { JAVA_PLUGIN_CALL_VOID_METHOD ,"JAVA_PLUGIN_CALL_VOID_METHOD"},
    { JAVA_PLUGIN_CALL_NV_METHOD ,"JAVA_PLUGIN_CALL_NV_METHOD"},
    { JAVA_PLUGIN_CALL_NV_OBJECT_METHOD ,"JAVA_PLUGIN_CALL_NV_OBJECT_METHOD"},
    { JAVA_PLUGIN_CALL_NV_BOOLEAN_METHOD,"JAVA_PLUGIN_CALL_NV_BOOLEAN_METHOD"},
    { JAVA_PLUGIN_CALL_NV_BYTE_METHOD ,"JAVA_PLUGIN_CALL_NV_BYTE_METHOD"},
    { JAVA_PLUGIN_CALL_NV_CHAR_METHOD ,"JAVA_PLUGIN_CALL_NV_CHAR_METHOD"},
    { JAVA_PLUGIN_CALL_NV_SHORT_METHOD ,"JAVA_PLUGIN_CALL_NV_SHORT_METHOD"},
    { JAVA_PLUGIN_CALL_NV_INT_METHOD ,"JAVA_PLUGIN_CALL_NV_INT_METHOD"},
    { JAVA_PLUGIN_CALL_NV_LONG_METHOD ,"JAVA_PLUGIN_CALL_NV_LONG_METHOD"},
    { JAVA_PLUGIN_CALL_NV_FLOAT_METHOD ,"JAVA_PLUGIN_CALL_NV_FLOAT_METHOD"},
    { JAVA_PLUGIN_CALL_NV_DOUBLE_METHOD ,"JAVA_PLUGIN_CALL_NV_DOUBLE_METHOD"},
    { JAVA_PLUGIN_CALL_NV_VOID_METHOD ,"JAVA_PLUGIN_CALL_NV_VOID_METHOD"},
    { JAVA_PLUGIN_GET_FIELD_ID ,"JAVA_PLUGIN_GET_FIELD_ID"},
    { JAVA_PLUGIN_GET_FIELD_BASE ,"JAVA_PLUGIN_GET_FIELD_BASE"},
    { JAVA_PLUGIN_GET_OBJECT_FIELD ,"JAVA_PLUGIN_GET_OBJECT_FIELD"},
    { JAVA_PLUGIN_GET_BOOLEAN_FIELD ,"JAVA_PLUGIN_GET_BOOLEAN_FIELD"},
    { JAVA_PLUGIN_GET_BYTE_FIELD ,"JAVA_PLUGIN_GET_BYTE_FIELD"},
    { JAVA_PLUGIN_GET_CHAR_FIELD ,"JAVA_PLUGIN_GET_CHAR_FIELD"},
    { JAVA_PLUGIN_GET_SHORT_FIELD ,"JAVA_PLUGIN_GET_SHORT_FIELD"},
    { JAVA_PLUGIN_GET_INT_FIELD ,"JAVA_PLUGIN_GET_INT_FIELD"},
    { JAVA_PLUGIN_GET_LONG_FIELD ,"JAVA_PLUGIN_GET_LONG_FIELD"},
    { JAVA_PLUGIN_GET_FLOAT_FIELD ,"JAVA_PLUGIN_GET_FLOAT_FIELD"},
    { JAVA_PLUGIN_GET_DOUBLE_FIELD ,"JAVA_PLUGIN_GET_DOUBLE_FIELD"},
    { JAVA_PLUGIN_SET_FIELD_BASE ,"JAVA_PLUGIN_SET_FIELD_BASE"},
    { JAVA_PLUGIN_SET_OBJECT_FIELD ,"JAVA_PLUGIN_SET_OBJECT_FIELD"},
    { JAVA_PLUGIN_SET_BOOLEAN_FIELD ,"JAVA_PLUGIN_SET_BOOLEAN_FIELD"},
    { JAVA_PLUGIN_SET_BYTE_FIELD ,"JAVA_PLUGIN_SET_BYTE_FIELD"},
    { JAVA_PLUGIN_SET_CHAR_FIELD ,"JAVA_PLUGIN_SET_CHAR_FIELD"},
    { JAVA_PLUGIN_SET_SHORT_FIELD ,"JAVA_PLUGIN_SET_SHORT_FIELD"},
    { JAVA_PLUGIN_SET_INT_FIELD ,"JAVA_PLUGIN_SET_INT_FIELD"},
    { JAVA_PLUGIN_SET_LONG_FIELD ,"JAVA_PLUGIN_SET_LONG_FIELD"},
    { JAVA_PLUGIN_SET_FLOAT_FIELD ,"JAVA_PLUGIN_SET_FLOAT_FIELD"},
    { JAVA_PLUGIN_SET_DOUBLE_FIELD ,"JAVA_PLUGIN_SET_DOUBLE_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_METHOD_ID ,"JAVA_PLUGIN_GET_STATIC_METHOD_ID"},
    { JAVA_PLUGIN_CALL_STATIC_METHOD ,"JAVA_PLUGIN_CALL_STATIC_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_OBJECT_METHOD ,"JAVA_PLUGIN_CALL_STATIC_OBJECT_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_BOOLEAN_METHOD ,"JAVA_PLUGIN_CALL_STATIC_BOOLEAN_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_BYTE_METHOD, "JAVA_PLUGIN_CALL_STATIC_BYTE_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_CHAR_METHOD, "JAVA_PLUGIN_CALL_STATIC_CHAR_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_SHORT_METHOD, "JAVA_PLUGIN_CALL_STATIC_SHORT_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_INT_METHOD,  "JAVA_PLUGIN_CALL_STATIC_INT_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_LONG_METHOD, "JAVA_PLUGIN_CALL_STATIC_LONG_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_FLOAT_METHOD , "JAVA_PLUGIN_CALL_STATIC_FLOAT_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_DOUBLE_METHOD ,"JAVA_PLUGIN_CALL_STATIC_DOUBLE_METHOD"},
    { JAVA_PLUGIN_CALL_STATIC_VOID_METHOD , "JAVA_PLUGIN_CALL_STATIC_VOID_METHOD"},
    { JAVA_PLUGIN_GET_STATIC_FIELD_ID ,"JAVA_PLUGIN_GET_STATIC_FIELD_ID"},
    { JP_GET_STATIC_FIELD_BASE ,"JP_GET_STATIC_FIELD_BASE"},
    { JAVA_PLUGIN_GET_STATIC_OBJECT_FIELD ,"JAVA_PLUGIN_GET_STATIC_OBJECT_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_BOOLEAN_FIELD ,"JAVA_PLUGIN_GET_STATIC_BOOLEAN_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_BYTE_FIELD,"JAVA_PLUGIN_GET_STATIC_BYTE_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_CHAR_FIELD,"JAVA_PLUGIN_GET_STATIC_CHAR_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_SHORT_FIELD,"JAVA_PLUGIN_GET_STATIC_SHORT_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_INT_FIELD ,"JAVA_PLUGIN_GET_STATIC_INT_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_LONG_FIELD ,"JAVA_PLUGIN_GET_STATIC_LONG_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_FLOAT_FIELD,"JAVA_PLUGIN_GET_STATIC_FLOAT_FIELD"},
    { JAVA_PLUGIN_GET_STATIC_DOUBLE_FIELD, "JAVA_PLUGIN_GET_STATIC_DOUBLE_FIELD"},
    { JP_SET_STATIC_FIELD_BASE ,"JP_SET_STATIC_FIELD_BASE"},
    { JAVA_PLUGIN_SET_STATIC_OBJECT_FIELD , "JAVA_PLUGIN_SET_STATIC_OBJECT_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_BOOLEAN_FIELD , "JAVA_PLUGIN_SET_STATIC_BOOLEAN_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_BYTE_FIELD ,"JAVA_PLUGIN_SET_STATIC_BYTE_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_CHAR_FIELD ,"JAVA_PLUGIN_SET_STATIC_CHAR_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_SHORT_FIELD,"JAVA_PLUGIN_SET_STATIC_SHORT_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_INT_FIELD ,"JAVA_PLUGIN_SET_STATIC_INT_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_LONG_FIELD ,"JAVA_PLUGIN_SET_STATIC_LONG_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_FLOAT_FIELD,"JAVA_PLUGIN_SET_STATIC_FLOAT_FIELD"},
    { JAVA_PLUGIN_SET_STATIC_DOUBLE_FIELD, "JAVA_PLUGIN_SET_STATIC_DOUBLE_FIELD"},
    { JAVA_PLUGIN_NEW_STRING ,"JAVA_PLUGIN_NEW_STRING"},
    { JAVA_PLUGIN_GET_STRING_SIZE ,"JAVA_PLUGIN_GET_STRING_SIZE"},
    { JAVA_PLUGIN_GET_STRING_CHARS ,"JAVA_PLUGIN_GET_STRING_CHARS"},
    { JAVA_PLUGIN_NEW_STRING_UTF ,"JAVA_PLUGIN_NEW_STRING_UTF"},
    { JAVA_PLUGIN_GET_STRING_UTF_SIZE ,"JAVA_PLUGIN_GET_STRING_UTF_SIZE"},
    { JAVA_PLUGIN_GET_STRING_UTF_CHARS ,"JAVA_PLUGIN_GET_STRING_UTF_CHARS"},
    { JAVA_PLUGIN_GET_ARRAY_LENGTH ,"JAVA_PLUGIN_GET_ARRAY_LENGTH"},
    { JAVA_PLUGIN_NEW_OBJECT_ARRAY ,"JAVA_PLUGIN_NEW_OBJECT_ARRAY"},
    { JAVA_PLUGIN_GET_OBJECT_ARRAY_ELEMENT, "JAVA_PLUGIN_GET_OBJECT_ARRAY_ELEMENT"},
    { JAVA_PLUGIN_SET_OBJECT_ARRAY_ELEMENT,"JAVA_PLUGIN_SET_OBJECT_ARRAY_ELEMENT"},
    { JAVA_PLUGIN_NEW_ARRAY_BASE ,"JAVA_PLUGIN_NEW_ARRAY_BASE"},
    { JAVA_PLUGIN_NEW_BOOL_ARRAY ,"JAVA_PLUGIN_NEW_BOOL_ARRAY"},
    { JAVA_PLUGIN_NEW_BYTE_ARRAY ,"JAVA_PLUGIN_NEW_BYTE_ARRAY"},
    { JAVA_PLUGIN_NEW_CHAR_ARRAY ,"JAVA_PLUGIN_NEW_CHAR_ARRAY"},
    { JAVA_PLUGIN_NEW_SHORT_ARRAY ,"JAVA_PLUGIN_NEW_SHORT_ARRAY"},
    { JAVA_PLUGIN_NEW_INT_ARRAY ,"JAVA_PLUGIN_NEW_INT_ARRAY"},
    { JAVA_PLUGIN_NEW_LONG_ARRAY ,"JAVA_PLUGIN_NEW_LONG_ARRAY"},
    { JAVA_PLUGIN_NEW_FLOAT_ARRAY ,"JAVA_PLUGIN_NEW_FLOAT_ARRAY"},
    { JAVA_PLUGIN_NEW_DOUBLE_ARRAY ,"JAVA_PLUGIN_NEW_DOUBLE_ARRAY"},
    { JAVA_PLUGIN_CAP_BASE ,"JAVA_PLUGIN_CAP_BASE"},
    { JAVA_PLUGIN_CAP_BOOL_AREL ,"JAVA_PLUGIN_CAP_BOOL_AREL"},
    { JAVA_PLUGIN_CAP_BYTE_AREL ,"JAVA_PLUGIN_CAP_BYTE_AREL"},
    { JAVA_PLUGIN_CAP_CHAR_AREL ,"JAVA_PLUGIN_CAP_CHAR_AREL"},
    { JAVA_PLUGIN_CAP_SHORT_AREL ,"JAVA_PLUGIN_CAP_SHORT_AREL"},
    { JAVA_PLUGIN_CAP_INT_AREL ,"JAVA_PLUGIN_CAP_INT_AREL"},
    { JAVA_PLUGIN_CAP_LONG_AREL ,"JAVA_PLUGIN_CAP_LONG_AREL"},
    { JAVA_PLUGIN_CAP_FLOAT_AREL ,"JAVA_PLUGIN_CAP_FLOAT_AREL"},
    { JAVA_PLUGIN_CAP_DOUBLE_AREL ,"JAVA_PLUGIN_CAP_DOUBLE_AREL"},
    { JAVA_PLUGIN_REL_BASE ,"JAVA_PLUGIN_REL_BASE"},
    { JAVA_PLUGIN_REL_BOOL_AREL ,"JAVA_PLUGIN_REL_BOOL_AREL"},
    { JAVA_PLUGIN_REL_BYTE_AREL ,"JAVA_PLUGIN_REL_BYTE_AREL"},
    { JAVA_PLUGIN_REL_CHAR_AREL ,"JAVA_PLUGIN_REL_CHAR_AREL"},
    { JAVA_PLUGIN_REL_SHORT_AREL ,"JAVA_PLUGIN_REL_SHORT_AREL"},
    { JAVA_PLUGIN_REL_INT_AREL ,"JAVA_PLUGIN_REL_INT_AREL"},
    { JAVA_PLUGIN_REL_LONG_AREL ,"JAVA_PLUGIN_REL_LONG_AREL"},
    { JAVA_PLUGIN_REL_FLOAT_AREL ,"JAVA_PLUGIN_REL_FLOAT_AREL"},
    { JAVA_PLUGIN_REL_DOUBLE_AREL ,"JAVA_PLUGIN_REL_DOUBLE_AREL"},
    { JAVA_PLUGIN_GET_BASE ,"JAVA_PLUGIN_GET_BASE"},
    { JAVA_PLUGIN_GET_BOOL_AREL ,"JAVA_PLUGIN_GET_BOOL_AREL"},
    { JAVA_PLUGIN_GET_BYTE_AREL ,"JAVA_PLUGIN_GET_BYTE_AREL"},
    { JAVA_PLUGIN_GET_CHAR_AREL ,"JAVA_PLUGIN_GET_CHAR_AREL"},
    { JAVA_PLUGIN_GET_SHORT_AREL ,"JAVA_PLUGIN_GET_SHORT_AREL"},
    { JAVA_PLUGIN_GET_INT_AREL ,"JAVA_PLUGIN_GET_INT_AREL"},
    { JAVA_PLUGIN_GET_LONG_AREL ,"JAVA_PLUGIN_GET_LONG_AREL"},
    { JAVA_PLUGIN_GET_FLOAT_AREL ,"JAVA_PLUGIN_GET_FLOAT_AREL"},
    { JAVA_PLUGIN_GET_DOUBLE_AREL ,"JAVA_PLUGIN_GET_DOUBLE_AREL"},
    { JAVA_PLUGIN_SET_BASE ,"JAVA_PLUGIN_SET_BASE"},
    { JAVA_PLUGIN_SET_BOOL_AREL ,"JAVA_PLUGIN_SET_BOOL_AREL"},
    { JAVA_PLUGIN_SET_BYTE_AREL ,"JAVA_PLUGIN_SET_BYTE_AREL"},
    { JAVA_PLUGIN_SET_CHAR_AREL ,"JAVA_PLUGIN_SET_CHAR_AREL"},
    { JAVA_PLUGIN_SET_SHORT_AREL ,"JAVA_PLUGIN_SET_SHORT_AREL"},
    { JAVA_PLUGIN_SET_INT_AREL ,"JAVA_PLUGIN_SET_INT_AREL"},
    { JAVA_PLUGIN_SET_LONG_AREL ,"JAVA_PLUGIN_SET_LONG_AREL"},
    { JAVA_PLUGIN_SET_FLOAT_AREL ,"JAVA_PLUGIN_SET_FLOAT_AREL"},
    { JAVA_PLUGIN_SET_DOUBLE_AREL ,"JAVA_PLUGIN_SET_DOUBLE_AREL"},
    { JAVA_PLUGIN_REGISTER_NATIVES ,"JAVA_PLUGIN_REGISTER_NATIVES"},
    { JAVA_PLUGIN_UNREGISTER_NATIVES ,"JAVA_PLUGIN_UNREGISTER_NATIVES"},
    { JAVA_PLUGIN_MONITOR_ENTER ,"JAVA_PLUGIN_MONITOR_ENTER"},
    { JAVA_PLUGIN_MONITOR_EXIT ,"JAVA_PLUGIN_MONITOR_EXIT"},
    { JAVA_PLUGIN_SECURE_NEW_OBJECT, "JAVA_PLUGIN_SECURE_NEW_OBJECT"},
    { JAVA_PLUGIN_SECURE_CALL, "JAVA_PLUGIN_SECURE_CALL"},
    { JAVA_PLUGIN_SECURE_CALL_NONVIRTUAL, "JAVA_PLUGIN_SECURE_CALL_NONVIRT"},
    { JAVA_PLUGIN_SECURE_GET_FIELD, "JAVA_PLUGIN_SECURE_GET_FIELD"},
    { JAVA_PLUGIN_SECURE_SET_FIELD, "JAVA_PLUGIN_SECURE_SET_FIELD"},
    { JAVA_PLUGIN_SECURE_CALL_STATIC, "JAVA_PLUGIN_SECURE_CALL_STATIC"},
    { JAVA_PLUGIN_SECURE_GET_STATIC_FIELD, "JAVA_PLUGIN_SECURE_GET_STATIC"},
    { JAVA_PLUGIN_SECURE_SET_STATIC_FIELD, "JAVA_PLUGIN_SECURE_SET_STATIC"},
    { JAVA_PLUGIN_GET_JAVA_OBJECT, "JAVA_PLUGIN_GET_JAVA_OBJECT" }
};



 
/* Method that converts the protocol descriptor p to a string */
char *protocol_descriptor_to_str(int p) {
    int i;
    for(i = 0; i < N_PROTOCOL_DESCRIPTORS; i++) {
	if (protocol_as_str[i].protocol == p) 
	    return protocol_as_str[i].name;
    }
    return "Unknown";
}



