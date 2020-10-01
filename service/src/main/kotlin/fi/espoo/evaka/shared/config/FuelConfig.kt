// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.github.kittinunf.fuel.core.FuelManager
import org.springframework.context.annotation.Configuration
import javax.net.ssl.SSLContext

@Configuration
class FuelConfig {
    init {
        val sc = SSLContext.getInstance("TLSv1.2")
        sc.init(null, null, null)
        FuelManager.instance.socketFactory = sc.socketFactory
    }
}
