// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.filter

import ch.qos.logback.access.spi.IAccessEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

class AccessLoggingFilter : Filter<IAccessEvent>() {
    override fun decide(event: IAccessEvent): FilterReply = when {
        event.request.requestURI == "/health" -> FilterReply.DENY
        event.request.requestURI == "/actuator/health" -> FilterReply.DENY
        else -> FilterReply.NEUTRAL
    }
}
