// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.pirkkala.mealintegration

import evaka.core.mealintegration.MealType
import evaka.core.mealintegration.MealTypeMapper

object PirkkalaMealTypeMapper : MealTypeMapper {
    override fun toMealId(mealType: MealType, specialDiet: Boolean): Int =
        if (specialDiet) {
            when (mealType) {
                MealType.BREAKFAST -> 504
                MealType.LUNCH -> 486
                MealType.LUNCH_PRESCHOOL -> 486
                MealType.SNACK -> 502
                MealType.SUPPER -> 509
                MealType.EVENING_SNACK -> 513
            }
        } else {
            when (mealType) {
                MealType.BREAKFAST -> 503
                MealType.LUNCH -> 485
                MealType.LUNCH_PRESCHOOL -> 485
                MealType.SNACK -> 501
                MealType.SUPPER -> 508
                MealType.EVENING_SNACK -> 512
            }
        }
}
