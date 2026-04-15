// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia.mealintagration

import evaka.core.mealintegration.MealType
import evaka.instance.nokia.mealintegration.NokiaMealTypeMapper
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class NokiaMealTypeMapper {

    @Test
    fun testNokiaMealTypeMapper() {
        val mapper = NokiaMealTypeMapper
        // Normal diet tests
        assertEquals(261, mapper.toMealId(MealType.BREAKFAST, false))
        assertEquals(273, mapper.toMealId(MealType.LUNCH, false))
        assertEquals(274, mapper.toMealId(MealType.SNACK, false))
        assertEquals(275, mapper.toMealId(MealType.SUPPER, false))
        assertEquals(306, mapper.toMealId(MealType.EVENING_SNACK, false))
        // Special diet tests
        assertEquals(288, mapper.toMealId(MealType.BREAKFAST, true))
        assertEquals(289, mapper.toMealId(MealType.LUNCH, true))
        assertEquals(290, mapper.toMealId(MealType.SNACK, true))
        assertEquals(291, mapper.toMealId(MealType.SUPPER, true))
        assertEquals(307, mapper.toMealId(MealType.EVENING_SNACK, true))
    }
}
