// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JamixSpecialDiet } from 'lib-common/generated/api-types/jamix'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MealTexture } from 'lib-common/generated/api-types/specialdiet'
import { SpecialDiet } from 'lib-common/generated/api-types/specialdiet'
import { client } from '../../api/client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.specialdiet.MealTexturesController.getMealTextures
*/
export async function getMealTextures(): Promise<MealTexture[]> {
  const { data: json } = await client.request<JsonOf<MealTexture[]>>({
    url: uri`/employee/meal-textures`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.specialdiet.SpecialDietController.getDiets
*/
export async function getDiets(): Promise<SpecialDiet[]> {
  const { data: json } = await client.request<JsonOf<SpecialDiet[]>>({
    url: uri`/employee/diets`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.specialdiet.SpecialDietController.putDiets
*/
export async function putDiets(
  request: {
    body: JamixSpecialDiet[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/diets`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<JamixSpecialDiet[]>
  })
  return json
}
