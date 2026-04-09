// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala.mealintagration

import evaka.core.mealintegration.MealType
import evaka.instance.lempaala.mealintegration.LempaalaMealTypeMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LempaalaMealTypeMapper {
    @Test
    fun testLempaalaMealTypeMapper() {
        val mapper = LempaalaMealTypeMapper
        // Normal diet tests
        assertEquals(578, mapper.toMealId(MealType.BREAKFAST, false))
        assertEquals(7, mapper.toMealId(MealType.LUNCH, false))
        assertEquals(20, mapper.toMealId(MealType.SNACK, false))
        assertEquals(24, mapper.toMealId(MealType.SUPPER, false))
        assertEquals(27, mapper.toMealId(MealType.EVENING_SNACK, false))
        // Special diet tests
        assertEquals(578, mapper.toMealId(MealType.BREAKFAST, true))
        assertEquals(8, mapper.toMealId(MealType.LUNCH, true))
        assertEquals(20, mapper.toMealId(MealType.SNACK, true))
        assertEquals(25, mapper.toMealId(MealType.SUPPER, true))
        assertEquals(27, mapper.toMealId(MealType.EVENING_SNACK, true))
    }
}
