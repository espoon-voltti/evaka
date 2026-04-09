// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.mealintegration

import evaka.core.mealintegration.MealType
import evaka.core.mealintegration.MealTypeMapper

class OrivesiMealTypeMapper : MealTypeMapper {
    override fun toMealId(mealType: MealType, specialDiet: Boolean): Int =
        if (specialDiet) {
            when (mealType) {
                MealType.BREAKFAST -> 752
                MealType.LUNCH -> 117
                MealType.LUNCH_PRESCHOOL -> 928
                MealType.SNACK -> 118
                MealType.SUPPER -> 753
                MealType.EVENING_SNACK -> 755
            }
        } else {
            when (mealType) {
                MealType.BREAKFAST -> 122
                MealType.LUNCH -> 68
                MealType.LUNCH_PRESCHOOL -> 927
                MealType.SNACK -> 70
                MealType.SUPPER -> 121
                MealType.EVENING_SNACK -> 80
            }
        }
}
