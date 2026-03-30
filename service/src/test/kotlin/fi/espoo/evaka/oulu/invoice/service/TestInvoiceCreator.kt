// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.invoice.service

import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.PersonDetailed
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun validInvoice(): InvoiceDetailed {
    val headOfFamily = validPerson()
    val invoiceRow1 =
        InvoiceRowDetailed(
            InvoiceRowId(UUID.randomUUID()),
            PersonDetailed(
                PersonId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
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
                PersonId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
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
            ProviderType.MUNICIPAL,
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
                PersonId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
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
            "kuvaus3",
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
        listOf(invoiceRow1, invoiceRow2, invoiceRow3),
        null,
        null,
        HelsinkiDateTime.Companion.of(LocalDateTime.of(2022, 5, 5, 1, 1)),
        relatedFeeDecisions = emptyList(),
        revisionNumber = 0,
        replacedInvoiceId = null,
        replacementNotes = null,
        replacementReason = null,
        attachments = emptyList(),
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
