// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import type { DaycareId } from 'lib-common/generated/api-types/shared'

import { getChildren } from '../generated/api-clients/attendance'

export async function getUnitChildren(
  unitId: DaycareId
): Promise<AttendanceChild[]> {
  return getChildren({ unitId }).then((data) =>
    data.sort((a, b) => compareByProperty(a, b, 'firstName'))
  )
}

function compareByProperty(
  a: AttendanceChild,
  b: AttendanceChild,
  property: 'firstName' | 'lastName'
) {
  if (a[property] < b[property]) {
    return -1
  }
  if (a[property] > b[property]) {
    return 1
  }
  return 0
}
