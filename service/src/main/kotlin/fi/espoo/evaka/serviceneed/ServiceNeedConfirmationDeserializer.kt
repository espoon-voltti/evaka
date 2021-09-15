// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

class ServiceNeedConfirmationDeserializer :
    StdDeserializer<ServiceNeedConfirmation>(ServiceNeedConfirmation::class.java) {

    private data class ServiceNeedConfirmationNullableFields(
        val employeeId: UUID?,
        val firstName: String?,
        val lastName: String?,
        val at: HelsinkiDateTime?
    )

    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): ServiceNeedConfirmation? {
        val confirmed = jp.readValueAs(ServiceNeedConfirmationNullableFields::class.java)
        if (confirmed.employeeId == null) {
            return null
        }
        return ServiceNeedConfirmation(confirmed.employeeId, confirmed.firstName!!, confirmed.lastName!!, confirmed.at)
    }
}
