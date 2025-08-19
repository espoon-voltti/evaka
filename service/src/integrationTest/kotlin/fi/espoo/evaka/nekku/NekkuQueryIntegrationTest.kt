// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NekkuQueryIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var nekkuController: NekkuController

    @Test
    fun `Meal types should contain the null value`() {
        val mealTypes =
            nekkuController.getNekkuMealTypes(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )

        assert(mealTypes.map { it.type }.contains(null))
    }

    @Test
    fun `Meal types should contain all NekkuProductMealTime values`() {
        val mealTypes =
            nekkuController.getNekkuMealTypes(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )

        NekkuProductMealType.entries.forEach { entry ->
            assert(mealTypes.map { it.type }.contains(entry))
        }
    }

    @Test
    fun `Fetching customer numbers should work`() {
        insertCustomerNumbers(db)

        val customerNumbers =
            nekkuController.getNekkuUnitNumbers(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(2, customerNumbers.size)
        assertEquals(
            listOf(
                NekkuUnitNumber("1234", "Lönnrotinkadun päiväkoti"),
                NekkuUnitNumber("5678", "Rubeberginkadun päiväkoti"),
            ),
            customerNumbers,
        )
    }

    @Test
    fun `Fetching special diets should work`() {
        insertSpecialDiets(db)

        val specialDiets =
            nekkuController.getNekkuSpecialDiets(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(1, specialDiets.size)
        assertEquals(listOf(NekkuSpecialDietWithoutFields("2", "Päiväkodit ER")), specialDiets)
    }

    @Test
    fun `Fetching special diet fields should work`() {
        insertSpecialDiets(db)

        val specialDietFields =
            nekkuController.getNekkuSpecialDietFields(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(2, specialDietFields.size)
        assertEquals(
            listOf(
                NekkuSpecialDietsFieldWithoutOptions(
                    "12345678-9ABC-DEF0-1234-56789ABCDEF0",
                    "Erityisruokavaliot",
                    NekkuSpecialDietType.CHECKBOXLIST,
                    "2",
                ),
                NekkuSpecialDietsFieldWithoutOptions(
                    "56789ABC-DEF0-1234-5678-9ABCDEF01234",
                    "Muu erityisruokavalio",
                    NekkuSpecialDietType.TEXT,
                    "2",
                ),
            ),
            specialDietFields,
        )
    }

    @Test
    fun `Fetching special diet options should work`() {
        insertSpecialDiets(db)

        val specialDietOptions =
            nekkuController.getNekkuSpecialDietOptions(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(2, specialDietOptions.size)
        assertEquals(
            listOf(
                NekkuSpecialDietOptionWithFieldId(
                    1,
                    "Kananmunaton",
                    "Kananmunaton",
                    "12345678-9ABC-DEF0-1234-56789ABCDEF0",
                ),
                NekkuSpecialDietOptionWithFieldId(
                    2,
                    "Sianlihaton",
                    "Sianlihaton",
                    "12345678-9ABC-DEF0-1234-56789ABCDEF0",
                ),
            ),
            specialDietOptions,
        )
    }

    @Test
    fun `Fetching the order reduction percentage by group should work`() {

        val area = DevCareArea()
        val daycare1 = DevDaycare(areaId = area.id, nekkuOrderReductionPercentage = 10)
        val dc1group1 = DevDaycareGroup(daycareId = daycare1.id)
        val dc1group2 = DevDaycareGroup(daycareId = daycare1.id)
        val daycare2 = DevDaycare(areaId = area.id, nekkuOrderReductionPercentage = 20)
        val dc2group = DevDaycareGroup(daycareId = daycare2.id)
        val daycare3 = DevDaycare(areaId = area.id, nekkuOrderReductionPercentage = 15)
        val dc3group = DevDaycareGroup(daycareId = daycare3.id)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare3)
            tx.insert(dc1group1)
            tx.insert(dc1group2)
            tx.insert(dc2group)
            tx.insert(dc3group)

            assertEquals(10, tx.getNekkuOrderReductionForDaycareByGroup(dc1group1.id))
            assertEquals(10, tx.getNekkuOrderReductionForDaycareByGroup(dc1group2.id))
            assertEquals(20, tx.getNekkuOrderReductionForDaycareByGroup(dc2group.id))
        }
    }
}
