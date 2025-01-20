// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { GroupId } from 'lib-common/generated/api-types/shared'
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
    end: LocalDate,
    groupIds?: GroupId[] | null
  }
): { url: Uri } {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()],
    ...(request.groupIds?.map((e): [string, string | null | undefined] => ['groupIds', e]) ?? [])
  )
  return {
    url: uri`${client.defaults.baseURL ?? ''}/employee/aromi`.appendQuery(params)
  }
}
