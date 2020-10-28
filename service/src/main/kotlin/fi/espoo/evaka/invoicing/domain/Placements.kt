// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

sealed class Placement(open val unit: UUID)

data class PermanentPlacement(
    override val unit: UUID,
    val type: PlacementType,
    val serviceNeed: ServiceNeed
) : Placement(unit)

data class TemporaryPlacement(override val unit: UUID, val partDay: Boolean) : Placement(unit)

data class PermanentPlacementWithHours(
    val unit: UUID,
    val type: PlacementType,
    val serviceNeed: ServiceNeed,
    val hours: Double?
) {
    fun withoutHours() = PermanentPlacement(this.unit, this.type, this.serviceNeed)
}

sealed class UnitData {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class JustId(val id: UUID) : UnitData()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InvoicedByMunicipality(val id: UUID, val invoicedByMunicipality: Boolean) : UnitData()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Detailed(val id: UUID, val name: String, val areaId: UUID, val areaName: String, val language: String) : UnitData()
}

enum class PlacementType(val freeHours: Double) {
    CLUB(0.0),
    DAYCARE(0.0),
    PRESCHOOL(20.0),
    PREPARATORY(25.0),
    PRESCHOOL_WITH_DAYCARE(20.0),
    PREPARATORY_WITH_DAYCARE(25.0),
    FIVE_YEARS_OLD_DAYCARE(20.0)
}

enum class ServiceNeed {
    MISSING,
    GTE_35,
    GTE_25,
    GT_25_LT_35,
    GT_15_LT_25,
    LTE_25,
    LTE_15,
    LTE_0
}

fun calculateServiceNeed(type: PlacementType, hours: Double?): ServiceNeed {
    return when (type) {
        PlacementType.FIVE_YEARS_OLD_DAYCARE,
        PlacementType.PRESCHOOL_WITH_DAYCARE,
        PlacementType.PREPARATORY_WITH_DAYCARE -> when {
            hours == null -> ServiceNeed.MISSING
            hours >= type.freeHours + 25 -> ServiceNeed.GTE_25
            hours > type.freeHours + 15 -> ServiceNeed.GT_15_LT_25
            hours > type.freeHours -> ServiceNeed.LTE_15
            else -> ServiceNeed.LTE_0
        }

        PlacementType.DAYCARE -> when {
            hours == null -> ServiceNeed.MISSING
            hours >= 35 -> ServiceNeed.GTE_35
            hours > 25 -> ServiceNeed.GT_25_LT_35
            else -> ServiceNeed.LTE_25
        }
        PlacementType.CLUB, PlacementType.PRESCHOOL, PlacementType.PREPARATORY -> ServiceNeed.LTE_0
    }
}
