// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.payment.service

class BicMapper {
    fun mapIban(iban: String): String {
        val normalizedIban = iban.filterNot { it.isWhitespace() }
        val validIbanRegex = Regex("FI[0-9]{16}", RegexOption.IGNORE_CASE)

        if (!validIbanRegex.matches(normalizedIban)) return "INVALID"

        val bankIdentifier = normalizedIban.subSequence(4, 7).toString().toInt()

        return when {
            bankIdentifier == 713 -> "CITIFIHX"
            bankIdentifier == 405 || bankIdentifier == 497 -> "HELSFIHH"
            bankIdentifier == 717 -> "BIGKFIH1"
            bankIdentifier >= 370 && bankIdentifier <= 379 -> "DNBAFIHX"
            bankIdentifier >= 500 && bankIdentifier <= 599 -> "OKOYFIHH"
            bankIdentifier >= 470 && bankIdentifier <= 479 -> "POPFFI22"
            bankIdentifier >= 340 && bankIdentifier <= 349 -> "DABAFIHH"
            bankIdentifier >= 800 && bankIdentifier <= 899 -> "DABAFIHH"
            bankIdentifier >= 310 && bankIdentifier <= 319 -> "HANDFIHH"
            bankIdentifier == 799 -> "HOLVFIHH"
            bankIdentifier >= 100 && bankIdentifier <= 299 -> "NDEAFIHH"
            bankIdentifier >= 330 && bankIdentifier <= 339 -> "ESSEFIHX"
            bankIdentifier >= 360 && bankIdentifier <= 369 -> "SBANFIHH"
            bankIdentifier >= 390 && bankIdentifier <= 399 -> "SBANFIHH"
            bankIdentifier >= 380 && bankIdentifier <= 389 -> "SWEDFIHH"
            bankIdentifier == 798 -> "VPAYFIH2"
            bankIdentifier >= 600 && bankIdentifier <= 699 -> "AABAFI22"
            bankIdentifier == 715 -> "ITELFIHH"
            bankIdentifier == 400 -> "ITELFIHH"
            bankIdentifier == 402 || bankIdentifier == 403 -> "ITELFIHH"
            bankIdentifier >= 406 && bankIdentifier <= 408 -> "ITELFIHH"
            bankIdentifier >= 410 && bankIdentifier <= 412 -> "ITELFIHH"
            bankIdentifier >= 414 && bankIdentifier <= 421 -> "ITELFIHH"
            bankIdentifier >= 423 && bankIdentifier <= 432 -> "ITELFIHH"
            bankIdentifier >= 435 && bankIdentifier <= 452 -> "ITELFIHH"
            bankIdentifier >= 454 && bankIdentifier <= 464 -> "ITELFIHH"
            bankIdentifier >= 483 && bankIdentifier <= 493 -> "ITELFIHH"
            bankIdentifier >= 495 && bankIdentifier <= 496 -> "ITELFIHH"
            else -> "UNKNOWN"
        }
    }
}
