// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI

@ConfigurationProperties(prefix = "fi.espoo.evaka.msg.sfi.ws")
@Component
data class SfiSoapProperties(
    var trustStore: KeystoreProperties = KeystoreProperties(),
    var keyStore: KeystoreProperties = KeystoreProperties(),
    var address: String? = ""
)

@ConfigurationProperties(prefix = "fi.espoo.evaka.msg.sfi.message")
@Component
data class SfiMessageProperties(
    var authorityIdentifier: String? = "",
    var serviceIdentifier: String? = "",
    var messageApiVersion: String = "1.1",
    var certificateCommonName: String? = ""
)

@ConfigurationProperties(prefix = "fi.espoo.evaka.msg.sfi.printing")
@Component
data class SfiPrintingProperties(
    var enablePrinting: Boolean = false,
    var forcePrintForElectronicUser: Boolean = false,
    var printingProvider: String? = "Edita",
    var billingId: String? = null,
    var billingPassword: String? = null

)

data class KeystoreProperties(
    var type: String = "pkcs12",
    var location: URI? = null,
    var password: String? = "",
    var signingKeyAlias: String? = "signing-key"
)
