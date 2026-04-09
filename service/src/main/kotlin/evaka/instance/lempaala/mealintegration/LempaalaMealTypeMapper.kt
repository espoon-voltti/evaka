// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala.mealintegration

import evaka.core.mealintegration.MealType
import evaka.core.mealintegration.MealTypeMapper

object LempaalaMealTypeMapper : MealTypeMapper {
    override fun toMealId(mealType: MealType, specialDiet: Boolean): Int =
        if (specialDiet) {
            when (mealType) {
                MealType.BREAKFAST -> 578
                MealType.LUNCH -> 8
                MealType.LUNCH_PRESCHOOL -> 8
                MealType.SNACK -> 20
                MealType.SUPPER -> 25
                MealType.EVENING_SNACK -> 27
            }
        } else {
            when (mealType) {
                MealType.BREAKFAST -> 578
                MealType.LUNCH -> 7
                MealType.LUNCH_PRESCHOOL -> 7
                MealType.SNACK -> 20
                MealType.SUPPER -> 24
                MealType.EVENING_SNACK -> 27
            }
        }
}
