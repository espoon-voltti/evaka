// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api-client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeControllerCitizen.getExpiringIncome
*/
export async function getExpiringIncome(): Promise<LocalDate[]> {
  const { data: json } = await client.request<JsonOf<LocalDate[]>>({
    url: uri`/citizen/income/expiring`.toString(),
    method: 'GET'
  })
  return json.map(e => LocalDate.parseIso(e))
}
