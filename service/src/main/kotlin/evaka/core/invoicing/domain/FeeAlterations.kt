@file:Suppress("ktlint:standard:filename")

// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import evaka.core.ConstList
import evaka.core.attachment.Attachment
import evaka.core.shared.ChildId
import evaka.core.shared.FeeAlterationId
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
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
    val modifiedAt: HelsinkiDateTime? = null,
    @Nested("modified_by") val modifiedBy: EvakaUser? = null,
    @Json val attachments: List<Attachment> = listOf(),
)

@ConstList("feeAlterationTypes")
enum class FeeAlterationType : DatabaseEnum {
    DISCOUNT,
    INCREASE,
    RELIEF;

    override val sqlType: String = "fee_alteration_type"
}
