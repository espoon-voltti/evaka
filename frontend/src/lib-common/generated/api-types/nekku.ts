// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

/**
* Generated from fi.espoo.evaka.nekku.NekkuMealType
*/
export interface NekkuMealType {
  name: string
  type: NekkuProductMealType | null
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuProductMealType
*/
export const nekku_product_meal_type = [
  'vegaani',
  'kasvis'
] as const

export type NekkuProductMealType = typeof nekku_product_meal_type[number]

/**
* Generated from fi.espoo.evaka.nekku.NekkuUnitNumber
*/
export interface NekkuUnitNumber {
  name: string
  number: string
}
