// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import LocalDate from 'lib-common/local-date'
import { ApplicationUnitType } from 'lib-common/generated/api-types/daycare'
import { JsonOf } from 'lib-common/json'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { client } from '../../api-client'
import { deserializeJsonPublicUnit } from 'lib-common/generated/api-types/daycare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.daycare.controllers.LocationController.getApplicationUnits
*/
export async function getApplicationUnits(
  request: {
    type: ApplicationUnitType,
    date: LocalDate,
    shiftCare: boolean | null
  }
): Promise<PublicUnit[]> {
  const { data: json } = await client.request<JsonOf<PublicUnit[]>>({
    url: uri`/public/units`.toString(),
    method: 'GET',
    params: {
      type: request.type,
      date: request.date.formatIso(),
      shiftCare: request.shiftCare
    }
  })
  return json.map(e => deserializeJsonPublicUnit(e))
}
