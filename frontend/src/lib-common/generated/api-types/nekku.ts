// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

/**
* Generated from evaka.core.nekku.NekkuMealType
*/
export interface NekkuMealType {
  name: string
  type: NekkuProductMealType | null
}

/**
* Generated from evaka.core.nekku.NekkuProductMealTime
*/
export type NekkuProductMealTime =
  | 'BREAKFAST'
  | 'LUNCH'
  | 'SNACK'
  | 'DINNER'
  | 'SUPPER'

/**
* Generated from evaka.core.nekku.NekkuProductMealType
*/
export const nekku_product_meal_type = [
  'VEGAN',
  'VEGETABLE'
] as const

export type NekkuProductMealType = typeof nekku_product_meal_type[number]

/**
* Generated from evaka.core.nekku.NekkuSpecialDietChoices
*/
export interface NekkuSpecialDietChoices {
  dietId: string
  fieldId: string
  value: string
}

/**
* Generated from evaka.core.nekku.NekkuSpecialDietOptionWithFieldId
*/
export interface NekkuSpecialDietOptionWithFieldId {
  fieldId: string
  key: string
  value: string
  weight: number
}

/**
* Generated from evaka.core.nekku.NekkuSpecialDietType
*/
export const nekku_special_diet_type = [
  'TEXT',
  'CHECKBOXLIST',
  'CHECKBOX',
  'RADIO',
  'TEXTAREA',
  'EMAIL'
] as const

export type NekkuSpecialDietType = typeof nekku_special_diet_type[number]

/**
* Generated from evaka.core.nekku.NekkuSpecialDietWithoutFields
*/
export interface NekkuSpecialDietWithoutFields {
  id: string
  name: string
}

/**
* Generated from evaka.core.nekku.NekkuSpecialDietsFieldWithoutOptions
*/
export interface NekkuSpecialDietsFieldWithoutOptions {
  diet_id: string
  id: string
  name: string
  type: NekkuSpecialDietType
}

/**
* Generated from evaka.core.nekku.NekkuUnitNumber
*/
export interface NekkuUnitNumber {
  name: string
  number: string
}
