// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala.mealintagration

import evaka.core.mealintegration.MealType
import evaka.instance.kangasala.mealintegration.KangasalaMealTypeMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KangasalaMealTypeMapper {
    @Test
    fun testKangasalaMealTypeMapper() {
        val mapper = KangasalaMealTypeMapper
        // Normal diet tests
        assertEquals(162, mapper.toMealId(MealType.BREAKFAST, false))
        assertEquals(175, mapper.toMealId(MealType.LUNCH, false))
        assertEquals(276, mapper.toMealId(MealType.LUNCH_PRESCHOOL, false))
        assertEquals(152, mapper.toMealId(MealType.SNACK, false))
        assertEquals(354, mapper.toMealId(MealType.SUPPER, false))
        assertEquals(188, mapper.toMealId(MealType.EVENING_SNACK, false))
        // Special diet tests
        assertEquals(143, mapper.toMealId(MealType.BREAKFAST, true))
        assertEquals(145, mapper.toMealId(MealType.LUNCH, true))
        assertEquals(277, mapper.toMealId(MealType.LUNCH_PRESCHOOL, true))
        assertEquals(160, mapper.toMealId(MealType.SNACK, true))
        assertEquals(173, mapper.toMealId(MealType.SUPPER, true))
        assertEquals(187, mapper.toMealId(MealType.EVENING_SNACK, true))
    }
}
