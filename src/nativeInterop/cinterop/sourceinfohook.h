#ifndef KOTLINTRACE_SOURCE_INFO_HOOK_H
#define KOTLINTRACE_SOURCE_INFO_HOOK_H

#include <TargetConditionals.h>

// Compile-time target detection: 1 on the iOS simulator (SDK defines
// TARGET_OS_SIMULATOR), 0 on device.
#if TARGET_OS_SIMULATOR
#define KOTLINTRACE_IS_SIMULATOR 1
#else
#define KOTLINTRACE_IS_SIMULATOR 0
#endif

// Kotlin/Native runtime global dispatcher for Throwable source info
// (writable function pointer, initially null, pointing at the platform
// backend after the runtime initializes it).
typedef int (*KotlinSourceInfoFn)(void *context, void *sourceInfo, int size);
extern KotlinSourceInfoFn Kotlin_getSourceInfo_Function;

// The runtime's bundled libbacktrace-based backend, which reads DWARF
// directly from the binary instead of using CoreSymbolication.
int Kotlin_getSourceInfo_libbacktrace(void *context, void *sourceInfo, int size);

#endif
