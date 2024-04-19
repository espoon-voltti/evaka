@file:Suppress("ktlint:standard:filename")

// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import org.jdbi.v3.json.Json

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeAlteration(
    val id: FeeAlterationId? = null,
    val personId: ChildId,
    val type: FeeAlterationType,
    val amount: Int,
    @get:JsonProperty("isAbsolute") val isAbsolute: Boolean,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val notes: String,
    val updatedAt: HelsinkiDateTime? = null,
    val updatedBy: EvakaUserId? = null,
    @Json val attachments: List<FeeAlterationAttachment> = listOf()
)

@ConstList("feeAlterationTypes")
enum class FeeAlterationType : DatabaseEnum {
    DISCOUNT,
    INCREASE,
    RELIEF;

    override val sqlType: String = "fee_alteration_type"
}

data class FeeAlterationAttachment(val id: AttachmentId, val name: String, val contentType: String)
