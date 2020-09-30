// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.pis.service.PersonDTO

private fun addressIsUnusable(streetAddress: String?, postalCode: String?, postOffice: String?): Boolean {
    return streetAddress.isNullOrBlank() || postalCode.isNullOrBlank() || postOffice.isNullOrBlank()
}

fun getSendAddress(guardian: PersonDTO, lang: String): DecisionSendAddress {
    val logMissingAddress = {
        logger.warn("Cannot deliver daycare decision to guardian ${guardian.id} with incomplete address. Using default decision address.")
    }
    return when {
        guardian.restrictedDetailsEnabled -> when (lang) {
            "sv" -> defaultAddressSv
            else -> defaultAddressFi
        }
        addressIsUnusable(guardian.streetAddress, guardian.postalCode, guardian.postOffice) -> {
            logMissingAddress()
            when (lang) {
                "sv" -> defaultAddressSv
                else -> defaultAddressFi
            }
        }
        else -> DecisionSendAddress(
            street = guardian.streetAddress!!,
            postalCode = guardian.postalCode!!,
            postOffice = guardian.postOffice!!,
            row1 = guardian.streetAddress,
            row2 = "${guardian.postalCode} ${guardian.postOffice}",
            row3 = ""
        )
    }
}

data class DecisionSendAddress(
    val street: String,
    val postalCode: String,
    val postOffice: String,
    val row1: String,
    val row2: String,
    val row3: String
)

val defaultAddressFi = DecisionSendAddress(
    street = "PL 3125",
    postalCode = "02070",
    postOffice = "Espoon kaupunki",
    row1 = "Varhaiskasvatuksen palveluohjaus",
    row2 = "PL 3125",
    row3 = "02070 Espoon kaupunki"
)

val defaultAddressSv = DecisionSendAddress(
    street = "PB 32",
    postalCode = "02070",
    postOffice = "ESBO STAD",
    row1 = "Svenska bildningstjänster",
    row2 = "Småbarnspedagogik",
    row3 = "PB 32, 02070 ESBO STAD"
)
