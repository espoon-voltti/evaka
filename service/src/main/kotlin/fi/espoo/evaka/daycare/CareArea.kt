// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.security.PilotFeature
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

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
    val id: DaycareId,
    val name: String,
    val openingDate: LocalDate?,
    val closingDate: LocalDate?,
    @Nested("care_area_") val area: DaycareCareArea,
    val type: Set<CareType>,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?,
    val providerType: ProviderType,
    val capacity: Int,
    val language: Language,
    val ghostUnit: Boolean,
    val uploadToVarda: Boolean,
    val uploadChildrenToVarda: Boolean,
    val uploadToKoski: Boolean,
    val invoicedByMunicipality: Boolean,
    val costCenter: String?,
    @Nested("finance_decision_handler_") val financeDecisionHandler: FinanceDecisionHandler?,
    val additionalInfo: String?,
    val phone: String?,
    val email: String?,
    val url: String?,
    @Nested("") val visitingAddress: VisitingAddress,
    val location: Coordinate?,
    @Nested("mailing_") val mailingAddress: MailingAddress,
    @Nested("unit_manager_") val unitManager: UnitManager,
    @Nested("decision_") val decisionCustomization: DaycareDecisionCustomization,
    val ophUnitOid: String?,
    val ophOrganizerOid: String?,
    val operationDays: Set<Int>,
    val roundTheClock: Boolean,
    val enabledPilotFeatures: List<PilotFeature>,
    val businessId: String,
    val iban: String,
    val providerId: String,
)

@PropagateNull("finance_decision_handler_id")
data class FinanceDecisionHandler(val id: EmployeeId, val firstName: String, val lastName: String)

data class UnitManager(val name: String?, val email: String?, val phone: String?)

data class DaycareDecisionCustomization(
    val daycareName: String,
    val preschoolName: String,
    val handler: String,
    val handlerAddress: String
)

data class DaycareCareArea(val id: AreaId, val name: String, val shortName: String)

@ConstList("careTypes")
enum class CareType {
    CLUB,
    FAMILY,
    CENTRE,
    GROUP_FAMILY,
    PRESCHOOL,
    PREPARATORY_EDUCATION
}

data class Location(
    val id: DaycareId,
    val name: String,
    val type: Set<CareType>,
    val care_area_id: AreaId,
    @Nested("") val visitingAddress: VisitingAddress,
    @Nested("mailing") val mailingAddress: MailingAddress,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?,
    val provider_type: ProviderType? = ProviderType.MUNICIPAL,
    val language: Language? = Language.fi,
    val location: Coordinate? = null,
    val phone: String? = null,
    val url: String? = null
)

data class UnitStub(val id: DaycareId, val name: String)

data class UnitFeatures(
    val id: DaycareId,
    val name: String,
    val features: List<PilotFeature>,
    val providerType: ProviderType,
    val type: List<CareType>
)
