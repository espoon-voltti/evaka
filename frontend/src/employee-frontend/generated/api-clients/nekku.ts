// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { GroupId } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { NekkuMealType } from 'lib-common/generated/api-types/nekku'
import type { NekkuSpecialDietOptionWithFieldId } from 'lib-common/generated/api-types/nekku'
import type { NekkuSpecialDietWithoutFields } from 'lib-common/generated/api-types/nekku'
import type { NekkuSpecialDietsFieldWithoutOptions } from 'lib-common/generated/api-types/nekku'
import type { NekkuUnitNumber } from 'lib-common/generated/api-types/nekku'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.nekku.NekkuController.getNekkuMealTypes
*/
export async function getNekkuMealTypes(): Promise<NekkuMealType[]> {
  const { data: json } = await client.request<JsonOf<NekkuMealType[]>>({
    url: uri`/employee/nekku/meal-types`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.nekku.NekkuController.getNekkuSpecialDietFields
*/
export async function getNekkuSpecialDietFields(): Promise<NekkuSpecialDietsFieldWithoutOptions[]> {
  const { data: json } = await client.request<JsonOf<NekkuSpecialDietsFieldWithoutOptions[]>>({
    url: uri`/employee/nekku/special-diet-fields`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.nekku.NekkuController.getNekkuSpecialDietOptions
*/
export async function getNekkuSpecialDietOptions(): Promise<NekkuSpecialDietOptionWithFieldId[]> {
  const { data: json } = await client.request<JsonOf<NekkuSpecialDietOptionWithFieldId[]>>({
    url: uri`/employee/nekku/special-diet-options`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.nekku.NekkuController.getNekkuSpecialDiets
*/
export async function getNekkuSpecialDiets(): Promise<NekkuSpecialDietWithoutFields[]> {
  const { data: json } = await client.request<JsonOf<NekkuSpecialDietWithoutFields[]>>({
    url: uri`/employee/nekku/special-diets`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.nekku.NekkuController.getNekkuUnitNumbers
*/
export async function getNekkuUnitNumbers(): Promise<NekkuUnitNumber[]> {
  const { data: json } = await client.request<JsonOf<NekkuUnitNumber[]>>({
    url: uri`/employee/nekku/unit-numbers`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.nekku.NekkuController.nekkuManualOrder
*/
export async function nekkuManualOrder(
  request: {
    groupId: GroupId,
    date: LocalDate
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['groupId', request.groupId],
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/nekku/manual-order`.toString(),
    method: 'POST',
    params
  })
  return json
}
