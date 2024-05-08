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
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.TimeRange
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
    override val name: String,
    val openingDate: LocalDate?,
    val closingDate: LocalDate?,
    @Nested("care_area_") val area: DaycareCareArea,
    val type: Set<CareType>,
    override val dailyPreschoolTime: TimeRange?,
    override val dailyPreparatoryTime: TimeRange?,
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
    val dwCostCenter: String?,
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
    override val operationDays: Set<Int>,
    val operationTimes: List<TimeRange?>,
    val shiftCareOperationDays: Set<Int>,
    val shiftCareOperationTimes: List<TimeRange?>,
    val roundTheClock: Boolean,
    val enabledPilotFeatures: List<PilotFeature>,
    val businessId: String,
    val iban: String,
    val providerId: String,
    @Nested("mealtime_") override val mealTimes: DaycareMealtimes
) : DaycareInfo

interface DaycareInfo {
    val name: String
    val operationDays: Set<Int>
    val dailyPreschoolTime: TimeRange?
    val dailyPreparatoryTime: TimeRange?
    val mealTimes: DaycareMealtimes
}

fun isUnitOperationDay(
    operationDays: Set<Int>,
    holidays: Set<LocalDate>,
    date: LocalDate
): Boolean {
    if (!operationDays.contains(date.dayOfWeek.value)) return false

    val isRoundTheClockUnit = operationDays == setOf(1, 2, 3, 4, 5, 6, 7)
    if (!isRoundTheClockUnit && holidays.contains(date)) {
        return false
    }

    return true
}

data class DaycareMealtimes(
    val breakfast: TimeRange?,
    val lunch: TimeRange?,
    val snack: TimeRange?,
    val supper: TimeRange?,
    val eveningSnack: TimeRange?
)

data class FinanceDecisionHandler(
    @PropagateNull val id: EmployeeId,
    val firstName: String,
    val lastName: String
)

data class UnitManager(val name: String, val email: String, val phone: String)

data class DaycareDecisionCustomization(
    val daycareName: String,
    val preschoolName: String,
    val handler: String,
    val handlerAddress: String
)

data class DaycareCareArea(val id: AreaId, val name: String, val shortName: String)

@ConstList("careTypes")
enum class CareType : DatabaseEnum {
    CLUB,
    FAMILY,
    CENTRE,
    GROUP_FAMILY,
    PRESCHOOL,
    PREPARATORY_EDUCATION;

    override val sqlType: String = "care_types"
}

data class UnitStub(val id: DaycareId, val name: String, val careTypes: List<CareType>)

data class UnitFeatures(
    val id: DaycareId,
    val name: String,
    val features: List<PilotFeature>,
    val providerType: ProviderType,
    val type: List<CareType>
)
