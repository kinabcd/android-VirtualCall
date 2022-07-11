/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.lospot.kin.call

/**
 * Manages logging for the entire class.
 */
object Log {
    private const val TAG = "KinCall"

    fun d(obj: Any, msg: String) {
        android.util.Log.d(TAG, delimit(getPrefix(obj)) + msg)
    }

    fun d(obj: Any, str1: String, str2: Any) {
        android.util.Log.d(TAG, delimit(getPrefix(obj)) + str1 + str2)
    }

    fun v(obj: Any, msg: String) {
        android.util.Log.v(TAG, delimit(getPrefix(obj)) + msg)
    }

    fun v(obj: Any, str1: String, str2: Any) {
        android.util.Log.d(TAG, delimit(getPrefix(obj)) + str1 + str2)
    }

    fun e(obj: Any, msg: String, e: Exception) {
        android.util.Log.e(TAG, delimit(getPrefix(obj)) + msg, e)
    }

    fun e(obj: Any, msg: String) {
        android.util.Log.e(TAG, delimit(getPrefix(obj)) + msg)
    }

    fun i(obj: Any, msg: String) {
        android.util.Log.i(TAG, delimit(getPrefix(obj)) + msg)
    }

    fun w(obj: Any, msg: String) {
        android.util.Log.w(TAG, delimit(getPrefix(obj)) + msg)
    }

    fun wtf(obj: Any, msg: String) {
        android.util.Log.wtf(TAG, delimit(getPrefix(obj)) + msg)
    }

    private fun getPrefix(obj: Any?): String = when (obj) {
        null -> ""
        is String -> obj
        else -> obj.javaClass.simpleName
    }

    private fun delimit(tag: String): String = "$tag - "
}
