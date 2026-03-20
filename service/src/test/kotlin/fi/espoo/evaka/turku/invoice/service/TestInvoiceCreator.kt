// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.invoice.service

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

fun validInvoiceZeroSum(): InvoiceDetailed {
    val headOfFamily = validPerson()
    val invoiceRow1 =
        InvoiceRowDetailed(
            InvoiceRowId(UUID.randomUUID()),
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(2018, 1, 1),
                null,
                "Jorma",
                "Heikäläinen",
                "210279-9988",
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
            0,
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 31),
            ProductKey("DAYCARE"),
            DaycareId(UUID.randomUUID()),
            "Satunnainen päiväkoti 2",
            ProviderType.MUNICIPAL,
            setOf(CareType.CENTRE),
            "112627",
            null,
            null,
            "kuvaus1",
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
        validCodebtor(),
        listOf(invoiceRow1),
        12345,
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
                "210281-9988",
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
            "Satunnainen päiväkoti 1",
            ProviderType.MUNICIPAL,
            setOf(CareType.CENTRE),
            "112627",
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
            "Satunnainen päiväkoti 2",
            ProviderType.MUNICIPAL,
            setOf(CareType.CENTRE),
            "",
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
            "Satunnainen päiväkoti 3",
            ProviderType.MUNICIPAL,
            setOf(CareType.FAMILY),
            "2627",
            null,
            null,
            "kuvaus3",
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
            -2500,
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 31),
            ProductKey("DAYCARE"),
            DaycareId(UUID.randomUUID()),
            "Satunnainen päiväkoti 3",
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
        validCodebtor(),
        listOf(invoiceRow1, invoiceRow2, invoiceRow3, invoiceRow4),
        12345,
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
        "Kirsi Marjatta Susanna Kirsi Marjatta Susanna Kirsi Marjatta Susanna",
        "Paavola-Mäkinen",
        "310382-956D",
        "Meikäläisenkuja Teikäläisenkuja Heikäläisenkuja 6 B 7",
        "20100",
        "TURKU",
        "",
        null,
        "",
        null,
        restrictedDetailsEnabled = false,
    )

fun validCodebtor() =
    PersonDetailed(
        PersonId(UUID.randomUUID()),
        LocalDate.of(1984, 3, 31),
        null,
        "Jorma Pertti Olavi Jorma Pertti Olavi Jorma Pertti Olavi",
        "Meikäläinen-Mäkinen",
        "310384-956D",
        "Meikäläisenkuja Teikäläisenkuja Heikäläisenkuja 6 B 7",
        "20100",
        "TURKU",
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
        "20100",
        "TURKU",
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
        "25600",
        "TURKU",
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
        "20200",
        "TURKU",
        "",
        null,
        "",
        null,
        restrictedDetailsEnabled = false,
    )
