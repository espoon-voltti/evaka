// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import com.github.kittinunf.fuel.core.FuelManager
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun FuelManager.Companion.trustAllCerts(): FuelManager {
    val fuel = FuelManager()
    val trustAllCerts =
        object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? = null

            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) = Unit

            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) = Unit
        }
    fuel.socketFactory =
        SSLContext
            .getInstance("SSL")
            .apply { init(null, arrayOf(trustAllCerts), java.security.SecureRandom()) }
            .socketFactory
    fuel.hostnameVerifier = HostnameVerifier { _, _ -> true }
    return fuel
}
