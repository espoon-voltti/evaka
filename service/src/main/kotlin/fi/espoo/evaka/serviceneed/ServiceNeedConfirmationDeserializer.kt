// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class ServiceNeedConfirmationDeserializer :
    StdDeserializer<ServiceNeedConfirmation>(ServiceNeedConfirmation::class.java) {

    private data class ServiceNeedConfirmationNullableFields(
        val userId: EvakaUserId?,
        val name: String?,
        val at: HelsinkiDateTime?,
    )

    override fun deserialize(
        jp: JsonParser,
        ctxt: DeserializationContext,
    ): ServiceNeedConfirmation? {
        val confirmed = jp.readValueAs(ServiceNeedConfirmationNullableFields::class.java)
        if (confirmed.userId == null) {
            return null
        }
        return ServiceNeedConfirmation(confirmed.userId, confirmed.name!!, confirmed.at)
    }
}
