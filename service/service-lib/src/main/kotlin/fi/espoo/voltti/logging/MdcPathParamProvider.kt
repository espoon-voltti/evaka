// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import net.logstash.logback.composite.AbstractJsonProvider
import tools.jackson.core.JsonGenerator

class MdcPathParamProvider : AbstractJsonProvider<ILoggingEvent>() {
    override fun writeTo(generator: JsonGenerator, event: ILoggingEvent) {
        val mdc = event.mdcPropertyMap
        if (mdc != null) {
            for ((key, value) in mdc) {
                if (key.startsWith("${MdcKey.HTTP_PATH_PARAM.key}.")) {
                    generator.writePOJOProperty(key, value)
                }
            }
        }
    }
}
