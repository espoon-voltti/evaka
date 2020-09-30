// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PlacementsTest {
    @Test
    fun `calculateServiceNeed when 40 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 40.0)

        Assertions.assertEquals(ServiceNeed.GTE_35, result)
    }

    @Test
    fun `calculateServiceNeed when 35 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 35.0)

        Assertions.assertEquals(ServiceNeed.GTE_35, result)
    }

    @Test
    fun `calculateServiceNeed when 34,9 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 34.9)

        Assertions.assertEquals(ServiceNeed.GT_25_LT_35, result)
    }

    @Test
    fun `calculateServiceNeed when 30 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 30.0)

        Assertions.assertEquals(ServiceNeed.GT_25_LT_35, result)
    }

    @Test
    fun `calculateServiceNeed when 25 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 25.0)

        Assertions.assertEquals(ServiceNeed.LTE_25, result)
    }

    @Test
    fun `calculateServiceNeed when 24,9 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 24.9)

        Assertions.assertEquals(ServiceNeed.LTE_25, result)
    }

    @Test
    fun `calculateServiceNeed when 20 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 20.0)

        Assertions.assertEquals(ServiceNeed.LTE_25, result)
    }

    @Test
    fun `calculateServiceNeed when 0 hours`() {
        val result = calculateServiceNeed(PlacementType.DAYCARE, 0.0)

        Assertions.assertEquals(ServiceNeed.LTE_25, result)
    }

    @Test
    fun `calculateServiceNeed for preschool`() {
        fun getNeed(hours: Double) = calculateServiceNeed(PlacementType.PRESCHOOL_WITH_DAYCARE, hours)
        Assertions.assertEquals(ServiceNeed.GTE_25, getNeed(45.0))
        Assertions.assertEquals(ServiceNeed.GT_15_LT_25, getNeed(44.5))
        Assertions.assertEquals(ServiceNeed.GT_15_LT_25, getNeed(35.5))
        Assertions.assertEquals(ServiceNeed.LTE_15, getNeed(35.0))
        Assertions.assertEquals(ServiceNeed.LTE_15, getNeed(20.5))
        Assertions.assertEquals(ServiceNeed.LTE_0, getNeed(20.0))
    }

    @Test
    fun `calculateServiceNeed for five years old`() {
        fun getNeed(hours: Double) = calculateServiceNeed(PlacementType.FIVE_YEARS_OLD_DAYCARE, hours)
        Assertions.assertEquals(ServiceNeed.GTE_25, getNeed(45.0))
        Assertions.assertEquals(ServiceNeed.GT_15_LT_25, getNeed(44.5))
        Assertions.assertEquals(ServiceNeed.GT_15_LT_25, getNeed(35.5))
        Assertions.assertEquals(ServiceNeed.LTE_15, getNeed(35.0))
        Assertions.assertEquals(ServiceNeed.LTE_15, getNeed(20.5))
        Assertions.assertEquals(ServiceNeed.LTE_0, getNeed(20.0))
    }

    @Test
    fun `calculateServiceNeed for preparatory education`() {
        fun getNeed(hours: Double) = calculateServiceNeed(PlacementType.PREPARATORY_WITH_DAYCARE, hours)
        Assertions.assertEquals(ServiceNeed.GTE_25, getNeed(50.0))
        Assertions.assertEquals(ServiceNeed.GT_15_LT_25, getNeed(49.5))
        Assertions.assertEquals(ServiceNeed.GT_15_LT_25, getNeed(40.5))
        Assertions.assertEquals(ServiceNeed.LTE_15, getNeed(40.0))
        Assertions.assertEquals(ServiceNeed.LTE_15, getNeed(25.5))
        Assertions.assertEquals(ServiceNeed.LTE_0, getNeed(25.0))
    }
}
