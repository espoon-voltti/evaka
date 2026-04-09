// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.mealintegration

import evaka.core.mealintegration.MealType
import evaka.core.mealintegration.MealTypeMapper

object YlojarviMealTypeMapper : MealTypeMapper {
    override fun toMealId(mealType: MealType, specialDiet: Boolean): Int =
        if (specialDiet) {
            when (mealType) {
                MealType.BREAKFAST -> 852
                MealType.LUNCH -> 854
                MealType.LUNCH_PRESCHOOL -> 856
                MealType.SNACK -> 858
                MealType.SUPPER -> 860
                MealType.EVENING_SNACK -> 862
            }
        } else {
            when (mealType) {
                MealType.BREAKFAST -> 851
                MealType.LUNCH -> 853
                MealType.LUNCH_PRESCHOOL -> 855
                MealType.SNACK -> 857
                MealType.SUPPER -> 859
                MealType.EVENING_SNACK -> 861
            }
        }
}
