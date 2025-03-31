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
  'VEGAN',
  'VEGETABLE'
] as const

export type NekkuProductMealType = typeof nekku_product_meal_type[number]

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDietChoices
*/
export interface NekkuSpecialDietChoices {
  dietId: string
  fieldId: string
  value: string
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDietOptionWithFieldId
*/
export interface NekkuSpecialDietOptionWithFieldId {
  fieldId: string
  key: string
  value: string
  weight: number
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDietType
*/
export const nekku_special_diet_type = [
  'TEXT',
  'CHECKBOXLIST'
] as const

export type NekkuSpecialDietType = typeof nekku_special_diet_type[number]

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDietWithoutFields
*/
export interface NekkuSpecialDietWithoutFields {
  id: string
  name: string
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDietsFieldWithoutOptions
*/
export interface NekkuSpecialDietsFieldWithoutOptions {
  diet_id: string
  id: string
  name: string
  type: NekkuSpecialDietType
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuUnitNumber
*/
export interface NekkuUnitNumber {
  name: string
  number: string
}
