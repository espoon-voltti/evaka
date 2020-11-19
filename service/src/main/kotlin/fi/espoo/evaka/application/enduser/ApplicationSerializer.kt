// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.enduser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.enduser.club.EnduserClubFormJSON
import fi.espoo.evaka.application.enduser.club.toEnduserJson
import fi.espoo.evaka.application.enduser.daycare.EnduserDaycareFormJSON
import fi.espoo.evaka.application.enduser.daycare.toEnduserDaycareJson
import fi.espoo.evaka.application.persistence.DatabaseForm
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import org.springframework.stereotype.Component

fun objectMapper(): ObjectMapper {
    val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    return mapper
}

@Component
class ApplicationSerializer {
    fun serialize(
        user: AuthenticatedUser,
        application: ApplicationDetails,
        hasOtherVtjGuardian: Boolean,
        guardiansLiveInSameAddress: Boolean
    ): ApplicationJson {
        val form = when (application.type) {
            ApplicationType.CLUB -> ClubFormV0.fromForm2(application.form, application.childRestricted, application.guardianRestricted).toEnduserJson()
            else -> DaycareFormV0.fromForm2(application.form, application.type, application.childRestricted, application.guardianRestricted).toEnduserDaycareJson()
        }

        return ApplicationJson(
            id = application.id,
            childId = application.childId,
            guardianId = application.guardianId,
            status = application.status,
            dueDate = application.dueDate,
            startDate = application.form.preferences.preferredStartDate,
            sentDate = application.sentDate,
            createdDate = application.createdDate,
            modifiedDate = application.modifiedDate,
            origin = application.origin,
            form = form,
            transferApplication = application.transferApplication,
            hasOtherVtjGuardian = hasOtherVtjGuardian,
            otherVtjGuardianHasSameAddress = guardiansLiveInSameAddress,
            otherGuardianAgreementStatus = application.form.secondGuardian?.agreementStatus
        )
    }

    fun deserialize(user: AuthenticatedUser, json: String): DatabaseForm {
        val node: JsonNode = objectMapper().readTree(json)["form"] ?: objectMapper().readTree(json)

        return when (val type = getType(node)) {
            ApplicationType.CLUB -> deserializeClubForm(node)
            ApplicationType.DAYCARE, ApplicationType.PRESCHOOL -> deserializeDaycareForm(node)
            else -> error("Can not deserialize application form with type $type")
        }.deserialize()
    }

    private fun deserializeClubForm(node: JsonNode): FormJson =
        objectMapper().treeToValue<EnduserClubFormJSON>(node)!!

    private fun deserializeDaycareForm(node: JsonNode): FormJson =
        objectMapper().treeToValue<EnduserDaycareFormJSON>(node)!!

    private fun getType(node: JsonNode): ApplicationType {
        val type = node.path("type")
        if (type.isNull) {
            error("Type is null!")
        }
        if (!type.isTextual) {
            error("Type is not textual.")
        }
        return ApplicationType.valueOf(type.asText().toUpperCase())
    }
}
