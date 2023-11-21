// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import org.slf4j.MDC

enum class MdcKey(val key: String) {
    SPAN_ID("spanId"),
    REQ_IP("userIp"),
    HTTP_METHOD("httpMethod"),
    PATH("path"),
    QUERY_STRING("queryString"),
    TRACE_ID("traceId"),
    USER_ID("userId"),
    USER_ID_HASH("userIdHash");

    fun get(): String? = MDC.get(this.key)
    fun set(value: String) = MDC.put(this.key, value)
    fun unset() = MDC.remove(this.key)
}
