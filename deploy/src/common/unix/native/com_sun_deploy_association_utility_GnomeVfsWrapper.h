/*
 * @(#)com_sun_deploy_association_utility_GnomeVfsWrapper.h	1.5 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_sun_deploy_association_utility_GnomeVfsWrapper */

#ifndef _Included_com_sun_deploy_association_utility_GnomeVfsWrapper
#define _Included_com_sun_deploy_association_utility_GnomeVfsWrapper
#ifdef __cplusplus
extern "C" { 
#endif
#undef com_sun_deploy_association_utility_GnomeVfsWrapper_GNOME_VFS_OK
#define com_sun_deploy_association_utility_GnomeVfsWrapper_GNOME_VFS_OK 0L

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_get_mime_type
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1get_1mime_1type
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_mime_get_description
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1mime_1get_1description
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_mime_get_icon
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1mime_1get_1icon
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_mime_get_key_list
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1mime_1get_1key_1list
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_mime_get_value
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1mime_1get_1value
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_mime_get_default_application_command
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1mime_1get_1default_1application_1command
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_get_registered_mime_types
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1get_1registered_1mime_1types
  (JNIEnv *, jclass);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    gnome_vfs_mime_get_extensions_list
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_gnome_1vfs_1mime_1get_1extensions_1list
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_sun_deploy_association_utility_GnomeVfsWrapper
 * Method:    getenv
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_getenv
  (JNIEnv *, jclass, jstring);

JNIEXPORT jboolean JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_openGNOMELibrary
  (JNIEnv *, jclass);


JNIEXPORT void JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_closeGNOMELibrary
  (JNIEnv *, jclass);


JNIEXPORT jboolean JNICALL Java_com_sun_deploy_association_utility_GnomeVfsWrapper_initGNOMELibrary
  (JNIEnv *, jclass);


#ifdef __cplusplus
}
#endif
#endif
