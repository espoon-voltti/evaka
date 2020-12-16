// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.Period
import org.jdbi.v3.core.mapper.Nested
import java.time.LocalDate
import java.util.UUID

data class VisitingAddress(
    val streetAddress: String = "", // address.street_address not nullable
    val postalCode: String = "", // address.postal_code not nullable
    val postOffice: String = ""

)

data class MailingAddress(
    val streetAddress: String? = null,
    val poBox: String? = null,
    val postalCode: String? = null,
    val postOffice: String? = null
)

data class Daycare(
    val id: UUID,
    val name: String,
    val openingDate: LocalDate?,
    val closingDate: LocalDate?,
    @Nested("care_area_")
    val area: DaycareCareArea,
    val type: Set<CareType>,
    val daycareApplyPeriod: Period?,
    val preschoolApplyPeriod: Period?,
    val clubApplyPeriod: Period?,
    val providerType: ProviderType,
    val capacity: Int,
    val language: Language,
    val ghostUnit: Boolean,
    val uploadToVarda: Boolean,
    val uploadToKoski: Boolean,
    val invoicedByMunicipality: Boolean,
    val costCenter: String?,
    val additionalInfo: String?,
    val phone: String?,
    val email: String?,
    val url: String?,
    @Nested("")
    val visitingAddress: VisitingAddress,
    val location: Coordinate?,
    @Nested("mailing_")
    val mailingAddress: MailingAddress,
    @Nested("unit_manager_")
    val unitManager: UnitManager,
    @Nested("decision_")
    val decisionCustomization: DaycareDecisionCustomization,
    val ophUnitOid: String?,
    val ophOrganizerOid: String?,
    val ophOrganizationOid: String?,
    val operationDays: Set<Int>
)

data class UnitManager(
    val name: String?,
    val email: String?,
    val phone: String?
)

data class DaycareDecisionCustomization(
    val daycareName: String,
    val preschoolName: String,
    val handler: String,
    val handlerAddress: String
)

data class DaycareCareArea(val id: UUID, val name: String, val shortName: String)

enum class CareType {
    CLUB, FAMILY, CENTRE, GROUP_FAMILY, PRESCHOOL, PREPARATORY_EDUCATION
}

data class Location(
    val id: UUID,
    val name: String,
    val type: Set<CareType>,
    val care_area_id: UUID,
    @Nested("")
    val visitingAddress: VisitingAddress,
    @Nested("mailing")
    val mailingAddress: MailingAddress,
    val daycareApplyPeriod: Period?,
    val preschoolApplyPeriod: Period?,
    val clubApplyPeriod: Period?,
    val provider_type: ProviderType? = ProviderType.MUNICIPAL,
    val language: Language? = Language.fi,
    val location: Coordinate? = null,
    val phone: String? = null,
    val url: String? = null
)

data class CareArea(
    val id: UUID,
    val name: String,
    val shortName: String,
    val locations: List<Location>
)

data class UnitStub(
    val id: UUID,
    val name: String
)
