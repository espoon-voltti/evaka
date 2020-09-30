// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI

@ConfigurationProperties(prefix = "fi.espoo.voltti.vtj.xroad")
@Component
data class XRoadProperties(
    var trustStore: KeystoreProperties = KeystoreProperties(),
    var keyStore: KeystoreProperties = KeystoreProperties(),
    var address: String? = "",
    var client: XroadClient = XroadClient(),
    var service: XroadService = XroadService(),
    // see https://esuomi.fi/palveluntarjoajille/palveluvayla/tekninen-aineisto/rajapintakuvaukset/x-road-tiedonsiirtoprotokolla/#4_SOAP-otsikkotiedot
    var protocolVersion: String = "4.0"
)

data class KeystoreProperties(
    var type: String = "pkcs12",
    var location: URI? = null,
    var password: String? = ""
)

data class XroadClient(
    var instance: String = "",
    var memberClass: String = "",
    var memberCode: String = "",
    var subsystemCode: String = ""
)

data class XroadService(
    var instance: String = "",
    var memberClass: String = "",
    var memberCode: String = "",
    var subsystemCode: String = "",
    var serviceCode: String = "",
    var serviceVersion: String? = null
)
