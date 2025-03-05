// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { Uri } from 'lib-common/uri'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.aromi.AromiController.getMealOrders
*/
export function getMealOrders(
  request: {
    start: LocalDate,
    end: LocalDate
  }
): { url: Uri } {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  return {
    url: uri`/employee/aromi`.withBaseUrl(client.defaults.baseURL ?? '').appendQuery(params)
  }
}
