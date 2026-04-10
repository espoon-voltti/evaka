// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia.invoice.service

import evaka.core.daycare.CareType
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.domain.InvoiceRowDetailed
import evaka.core.invoicing.domain.InvoiceStatus
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.invoicing.service.ProductKey
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.InvoiceId
import evaka.core.shared.InvoiceRowId
import evaka.core.shared.PersonId
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun validInvoice(): InvoiceDetailed {
    val headOfFamily = validPerson()
    val invoiceRow1 =
        InvoiceRowDetailed(
            InvoiceRowId(UUID.randomUUID()),
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(2018, 1, 1),
                null,
                "Matti",
                "Meikäläinen",
                null,
                "",
                "",
                "",
                "",
                null,
                "",
                null,
                restrictedDetailsEnabled = false,
            ),
            1,
            24300,
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 31),
            ProductKey("DAYCARE"),
            DaycareId(UUID.randomUUID()),
            "",
            ProviderType.MUNICIPAL,
            setOf(CareType.CENTRE),
            "2627",
            null,
            null,
            "kuvaus1",
            correctionId = null,
            note = null,
        )
    val invoiceRow2 =
        InvoiceRowDetailed(
            InvoiceRowId(UUID.randomUUID()),
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(2015, 11, 26),
                null,
                "Maiju",
                "Meikäläinen",
                null,
                "",
                "",
                "",
                "",
                null,
                "",
                null,
                restrictedDetailsEnabled = false,
            ),
            1,
            48200,
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 31),
            ProductKey("PRESCHOOL_WITH_DAYCARE"),
            DaycareId(UUID.randomUUID()),
            "",
            ProviderType.PURCHASED,
            setOf(CareType.CENTRE),
            "2627",
            null,
            null,
            "kuvaus2",
            correctionId = null,
            note = null,
        )
    val invoiceRow3 =
        InvoiceRowDetailed(
            InvoiceRowId(UUID.randomUUID()),
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(2018, 1, 1),
                null,
                "Matti",
                "Meikäläinen",
                null,
                "",
                "",
                "",
                "",
                null,
                "",
                null,
                restrictedDetailsEnabled = false,
            ),
            1,
            25000,
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 31),
            ProductKey("DAYCARE"),
            DaycareId(UUID.randomUUID()),
            "",
            ProviderType.MUNICIPAL,
            setOf(CareType.FAMILY),
            "2627",
            null,
            null,
            "",
            correctionId = null,
            note = null,
        )
    val invoiceRow4 =
        InvoiceRowDetailed(
            InvoiceRowId(UUID.randomUUID()),
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(2018, 1, 1),
                null,
                "Matti",
                "Meikäläinen",
                null,
                "",
                "",
                "",
                "",
                null,
                "",
                null,
                restrictedDetailsEnabled = false,
            ),
            1,
            -25000,
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 31),
            ProductKey("FREE_OF_CHARGE"),
            DaycareId(UUID.randomUUID()),
            "",
            ProviderType.MUNICIPAL,
            setOf(CareType.FAMILY),
            "2627",
            null,
            null,
            "kuvaus4",
            correctionId = null,
            note = null,
        )
    return InvoiceDetailed(
        (InvoiceId(UUID.randomUUID())),
        InvoiceStatus.WAITING_FOR_SENDING,
        LocalDate.of(2022, 5, 5),
        LocalDate.of(2022, 5, 5),
        LocalDate.of(2021, 3, 6),
        LocalDate.of(2021, 2, 4),
        null,
        AreaId(UUID.randomUUID()),
        headOfFamily,
        null,
        listOf(invoiceRow1, invoiceRow2, invoiceRow3, invoiceRow4),
        null,
        null,
        HelsinkiDateTime.Companion.of(LocalDateTime.of(2022, 5, 5, 1, 1)),
        emptyList(),
        0,
        null,
        null,
        null,
        emptyList(),
    )
}

fun validPerson() =
    PersonDetailed(
        PersonId(UUID.randomUUID()),
        LocalDate.of(1982, 3, 31),
        null,
        "Matti",
        "Meikäläinen",
        "310382-956D",
        "Meikäläisenkuja 6 B 7",
        "90100",
        "OULU",
        "",
        null,
        "",
        null,
        restrictedDetailsEnabled = false,
    )

fun personWithoutSSN() =
    PersonDetailed(
        PersonId(UUID.randomUUID()),
        LocalDate.of(1982, 3, 31),
        null,
        "Maija",
        "Meikäläinen",
        null,
        "Meikäläisenkuja 6 B 7",
        "90100",
        "OULU",
        "",
        null,
        "",
        null,
        restrictedDetailsEnabled = false,
    )

fun personWithRestrictedDetails() =
    PersonDetailed(
        PersonId(UUID.randomUUID()),
        LocalDate.of(1982, 3, 31),
        null,
        "Mysteeri",
        "Meikäläinen",
        "280691-943Z",
        "Todistajansuojeluohjelmankatu 9",
        "45600",
        "OULU",
        "",
        null,
        "",
        null,
        restrictedDetailsEnabled = true,
    )

fun personWithLongName() =
    PersonDetailed(
        PersonId(UUID.randomUUID()),
        LocalDate.of(1982, 3, 31),
        null,
        "Eskoensio Velipekka-Simopetteri",
        "von und zu Aaltonen-Räyhäkäinen",
        "310382-956D",
        "Aateliskulma 3",
        "90200",
        "OULU",
        "",
        null,
        "",
        null,
        restrictedDetailsEnabled = false,
    )
