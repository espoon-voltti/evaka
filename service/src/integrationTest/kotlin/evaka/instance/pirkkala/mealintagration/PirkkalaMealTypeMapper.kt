// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.pirkkala.mealintagration

import evaka.core.mealintegration.MealType
import evaka.instance.pirkkala.mealintegration.PirkkalaMealTypeMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PirkkalaMealTypeMapper {

    @Test
    fun testPirkkalaMealTypeMapper() {
        val mapper = PirkkalaMealTypeMapper
        // Normal diet tests
        assertEquals(503, mapper.toMealId(MealType.BREAKFAST, false))
        assertEquals(485, mapper.toMealId(MealType.LUNCH, false))
        assertEquals(501, mapper.toMealId(MealType.SNACK, false))
        assertEquals(508, mapper.toMealId(MealType.SUPPER, false))
        assertEquals(512, mapper.toMealId(MealType.EVENING_SNACK, false))
        // Special diet tests
        assertEquals(504, mapper.toMealId(MealType.BREAKFAST, true))
        assertEquals(486, mapper.toMealId(MealType.LUNCH, true))
        assertEquals(502, mapper.toMealId(MealType.SNACK, true))
        assertEquals(509, mapper.toMealId(MealType.SUPPER, true))
        assertEquals(513, mapper.toMealId(MealType.EVENING_SNACK, true))
    }
}
