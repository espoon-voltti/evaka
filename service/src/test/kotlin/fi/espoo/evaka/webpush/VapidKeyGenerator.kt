// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

fun main() {
    val keyPair = WebPushCrypto.generateKeyPair()
    println("Generated key pair: $keyPair")
    println("Public key: ${keyPair.publicKeyBase64()}")
    println("Private key: ${keyPair.privateKeyBase64()}")
}
