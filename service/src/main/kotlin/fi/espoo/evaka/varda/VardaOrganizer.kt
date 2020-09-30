// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.varda.integration.VardaClient
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.util.UUID

fun updateOrganizer(h: Handle, client: VardaClient) {
    val organizer = getStaleOrganizer(h)

    organizer?.let {
        client.updateOrganizer(organizer.toUpdateObject()).let { success ->
            if (success) setOrganizerUploaded(organizer, h)
        }
    }
}

fun getStaleOrganizer(h: Handle): VardaOrganizer? {
    //language=SQL
    val sql =
        """
        SELECT id, varda_organizer_id, varda_organizer_oid, url, email, phone, iban, municipality_code
        FROM varda_organizer
        WHERE updated_at > uploaded_at OR uploaded_at IS NULL
        """.trimIndent()
    return h.createQuery(sql).mapTo<VardaOrganizer>().firstOrNull()
}

fun setOrganizerUploaded(organizer: VardaOrganizer, h: Handle) {
    //language=SQL
    val sql =
        """
        UPDATE varda_organizer
        SET uploaded_at = now()
        WHERE id = :id
        """.trimIndent()
    h.createUpdate(sql)
        .bind("id", organizer.id)
        .execute()
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaOrganizer(
    val id: UUID,
    val vardaOrganizerId: Long,
    val vardaOrganizerOid: String?,
    val url: String?,
    val email: String?,
    val phone: String?,
    val iban: String?,
    val municipalityCode: String?,
    val uploadedAt: Instant? = null
) {
    fun toUpdateObject(): VardaUpdateOrganizer = VardaUpdateOrganizer(vardaOrganizerId, email, phone, iban)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaOrganizerResponse(
    @JsonProperty("id")
    val vardaOrganizerId: Long,
    @JsonProperty("organisaatio_oid")
    val vardaOrganizerOid: String,
    @JsonProperty("url")
    val url: String,
    @JsonProperty("kunta_koodi")
    val municipalityCode: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaUpdateOrganizer(
    @JsonIgnore
    val vardaOrganizerId: Long,
    @JsonProperty("sahkopostiosoite")
    val email: String?,
    @JsonProperty("puhelinnumero")
    val phone: String?,
    @JsonProperty("tilinumero")
    val iban: String?
)
