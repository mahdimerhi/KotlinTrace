// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sample

import kotlin.concurrent.thread

internal actual fun crashOnBackgroundThread(block: () -> Unit) {
    thread(block = block)
}
