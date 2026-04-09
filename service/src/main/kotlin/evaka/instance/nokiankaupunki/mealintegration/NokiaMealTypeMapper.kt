// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokiankaupunki.mealintegration

import evaka.core.mealintegration.MealType
import evaka.core.mealintegration.MealTypeMapper

object NokiaMealTypeMapper : MealTypeMapper {
    override fun toMealId(mealType: MealType, specialDiet: Boolean): Int =
        if (specialDiet) {
            when (mealType) {
                MealType.BREAKFAST -> 288
                MealType.LUNCH -> 289
                MealType.LUNCH_PRESCHOOL -> 289
                MealType.SNACK -> 290
                MealType.SUPPER -> 291
                MealType.EVENING_SNACK -> 307
            }
        } else {
            when (mealType) {
                MealType.BREAKFAST -> 261
                MealType.LUNCH -> 273
                MealType.LUNCH_PRESCHOOL -> 273
                MealType.SNACK -> 274
                MealType.SUPPER -> 275
                MealType.EVENING_SNACK -> 306
            }
        }
}
