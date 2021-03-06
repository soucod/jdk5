/*
 * @(#)UnixOperatingSystem_md.c	1.2 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"
#include "management.h"
#include "com_sun_management_UnixOperatingSystem.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/swap.h>
#include <sys/resource.h>
#include <sys/times.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdlib.h>
#include <unistd.h>

static jlong page_size = 0;

/* This gets us the new structured proc interfaces of 5.6 & later */
/* - see comment in <sys/procfs.h> */
#define _STRUCTURED_PROC 1  
#include <sys/procfs.h>     

static struct dirent* read_dir(DIR* dirp, struct dirent* entry) {
#ifdef __solaris__
    struct dirent* dbuf = readdir(dirp);
    return dbuf;
#else /* __linux__ */
    struct dirent* p;
    if (readdir_r(dirp, entry, &p) == 0) {
        return p;
    } else {
        return NULL;
    }
#endif
}

// true = get available swap in bytes
// false = get total swap in bytes
static jlong get_total_or_available_swap_space_size(JNIEnv* env, jboolean available) {
#ifdef __solaris__
    long total, avail;
    int nswap, i, count;
    swaptbl_t *stbl;
    char *strtab;
  
    // First get the number of swap resource entries
    if ((nswap = swapctl(SC_GETNSWP, NULL)) == -1) {
        throw_internal_error(env, "swapctl failed to get nswap");
        return -1;
    }
    if (nswap == 0) {
        return 0;
    }
  
    // Allocate storage for resource entries
    stbl = (swaptbl_t*) malloc(nswap * sizeof(swapent_t) +
                               sizeof(struct swaptable));
    if (stbl == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return -1;
    }
  
    // Allocate storage for the table
    strtab = (char*) malloc((nswap + 1) * MAXPATHLEN);
    if (strtab == NULL) {
        free(stbl);
        JNU_ThrowOutOfMemoryError(env, 0);
        return -1;
    }

    for (i = 0; i < (nswap + 1); i++) {
      stbl->swt_ent[i].ste_path = strtab + (i * MAXPATHLEN);
    }
    stbl->swt_n = nswap + 1;
  
    // Get the entries
    if ((count = swapctl(SC_LIST, stbl)) < 0) {
        free(stbl);
        free(strtab);
        throw_internal_error(env, "swapctl failed to get swap list");
        return -1;
    }
  
    // Sum the entries to get total and free swap
    total = 0;
    avail = 0;
    for (i = 0; i < count; i++) {
      total += stbl->swt_ent[i].ste_pages;
      avail += stbl->swt_ent[i].ste_free;
    }
  
    free(stbl);
    free(strtab);
    return available ? ((jlong)avail * page_size) :
                       ((jlong)total * page_size);
#else /* __linux__ */
    FILE *fp;
    unsigned long total = 0, avail = 0;
   
    if ((fp = fopen("/proc/meminfo", "r")) == NULL) {
        throw_internal_error(env, "Unable to open /proc/meminfo");
        return -1;
    }
  
    // Skip first 2 lines
    // Third line should look like:
    // "Swap: <total> <used> <free>"
    if (fscanf(fp, "%*[^\n]\n") == EOF ||
        fscanf(fp, "%*[^\n]\n") == EOF ||
        fscanf(fp, "%*s %lu %*lu %lu\n", &total, &avail) == EOF) {
        throw_internal_error(env, "Unable to get swap info");
        fclose(fp);
        return -1;
    }
  
    fclose(fp);
    return available ? (jlong)avail : (jlong)total;
#endif
}

JNIEXPORT void JNICALL
Java_com_sun_management_UnixOperatingSystem_initialize
  (JNIEnv *env, jclass cls)
{
    page_size = sysconf(_SC_PAGESIZE);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getCommittedVirtualMemorySize
  (JNIEnv *env, jobject mbean)
{
#ifdef __solaris__
    psinfo_t psinfo;
    ssize_t result;
    size_t remaining;
    char* addr;
    int fd;
  
    fd = JVM_Open("/proc/self/psinfo", O_RDONLY, 0);
    if (fd < 0) {
        throw_internal_error(env, "Unable to open /proc/self/psinfo");
        return -1;
    }
  
    addr = (char *)&psinfo;
    for (remaining = sizeof(psinfo_t); remaining > 0;) {
        result = JVM_Read(fd, addr, remaining);
        if (result < 0) {
            JVM_Close(fd);
            throw_internal_error(env, "Unable to read /proc/self/psinfo");
            return -1;
        }
        remaining -= result;
        addr += result;
    }
  
    JVM_Close(fd);
    return (jlong) psinfo.pr_size * 1024;
#else /* __linux__ */
    FILE *fp;
    unsigned long vsize = 0;
  
    if ((fp = fopen("/proc/self/stat", "r")) == NULL) {
        throw_internal_error(env, "Unable to open /proc/self/stat");
        return -1;
    }
  
    // Ignore everything except the vsize entry
    if (fscanf(fp, "%*d %*s %*c %*d %*d %*d %*d %*d %*u %*u %*u %*u %*u %*d %*d %*d %*d %*d %*d %*u %*u %*d %lu %*[^\n]\n", &vsize) == EOF) {
        throw_internal_error(env, "Unable to get virtual memory usage");
        fclose(fp);
        return -1;
    }
  
    fclose(fp);
    return (jlong)vsize;
#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getTotalSwapSpaceSize
  (JNIEnv *env, jobject mbean)
{
    return get_total_or_available_swap_space_size(env, JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getFreeSwapSpaceSize
  (JNIEnv *env, jobject mbean)
{
    return get_total_or_available_swap_space_size(env, JNI_TRUE);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getProcessCpuTime
  (JNIEnv *env, jobject mbean)
{
    jlong clk_tck, ns_per_clock_tick;
    jlong cpu_time_ns;
    struct tms time;

#ifdef __solaris__
    clk_tck = (jlong) sysconf(_SC_CLK_TCK);
#else /* __linux__ */
    clk_tck = 100;
#endif
    if (clk_tck == -1) {
        throw_internal_error(env, 
                             "sysconf failed - not able to get clock tick");
        return -1;
    }

    times(&time);
    ns_per_clock_tick = (jlong) 1000 * 1000 * 1000 / (jlong) clk_tck;
    cpu_time_ns = ((jlong)time.tms_utime + (jlong) time.tms_stime) * 
                      ns_per_clock_tick;
    return cpu_time_ns;
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getFreePhysicalMemorySize
  (JNIEnv *env, jobject mbean)
{
    jlong num_avail_physical_pages = sysconf(_SC_AVPHYS_PAGES);
    return (num_avail_physical_pages * page_size);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getTotalPhysicalMemorySize
  (JNIEnv *env, jobject mbean)
{
    jlong num_physical_pages = sysconf(_SC_PHYS_PAGES);
    return (num_physical_pages * page_size);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getOpenFileDescriptorCount
  (JNIEnv *env, jobject mbean)
{
    DIR *dirp;
    struct dirent dbuf;
    struct dirent* dentp;
    jlong fds = 0;
   
    dirp = opendir("/proc/self/fd");
    if (dirp == NULL) {
        throw_internal_error(env, "Unable to open directory /proc/self/fd");
        return -1;
    }
  
    // iterate through directory entries, skipping '.' and '..'
    // each entry represents an open file descriptor.
    while ((dentp = read_dir(dirp, &dbuf)) != NULL) {
        if (isdigit(dentp->d_name[0])) {
            fds++;
        }
    }
  
    closedir(dirp);
    // subtract by 1 which was the fd open for this implementation
    return (fds - 1);
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_UnixOperatingSystem_getMaxFileDescriptorCount
  (JNIEnv *env, jobject mbean)
{
    struct rlimit rlp;

    if (getrlimit(RLIMIT_NOFILE, &rlp) == -1) {
        throw_internal_error(env, "getrlimit failed");
        return -1;
    }
    return (jlong) rlp.rlim_cur;
}
