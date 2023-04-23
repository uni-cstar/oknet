/*
 * Copyright  2023 ,luochao
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

package unicstar.oknet.okhttp

import okhttp3.Headers
import okhttp3.Request

/**
 * the strategy when the expect set header in original request.
 */
sealed class OnConflictStrategy {

    /**
     * @param builder
     * @param key the key of header
     * @param value the value of header
     */
    abstract fun apply(
        originalHeaders: Headers,
        builder: Request.Builder,
        key: String,
        value: String
    )

    internal fun Headers?.contains(key: String): Boolean {
        if (this == null)
            return false
        return key.isNotEmpty() && !this.get(key).isNullOrEmpty()
    }

    /**
     * if header is exists in original request,ignore(do nothing).
     */
    object IGNORE : OnConflictStrategy() {
        override fun apply(
            originalHeaders: Headers,
            builder: Request.Builder,
            key: String,
            value: String
        ) {
            if (originalHeaders.contains(key))
                return
            builder.addHeader(key,value)
        }
    }

    /**
     * if the header is exist in origal request,replace it.
     */
    object REPLACE : OnConflictStrategy() {
        override fun apply(
            originalHeaders: Headers,
            builder: Request.Builder,
            key: String,
            value: String
        ) {
            builder.header(key, value)
        }
    }

    /**
     * add header directly,because the request allow add same headers.
     */
    object ADD : OnConflictStrategy() {
        override fun apply(
            originalHeaders: Headers,
            builder: Request.Builder,
            key: String,
            value: String
        ) {
            builder.addHeader(key, value)
        }
    }

    /**
     * if the header is exits in original request ,throw IllegalStateException.
     */
    object ABORT : OnConflictStrategy() {
        override fun apply(
            originalHeaders: Headers,
            builder: Request.Builder,
            key: String,
            value: String
        ) {
            if (originalHeaders.contains(key))
                throw IllegalStateException("the header(key=$key,value=$value) is exits in original request,and the OnConflictStrategy is set Abort.")
            builder.header(key, value)//use set rather than use addHeader method
        }
    }
}