// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { StaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from './client'

export function getStaffAttendances(
  unitId: UUID,
  range: FiniteDateRange
): Promise<Result<StaffAttendanceResponse>> {
  return client
    .get<JsonOf<StaffAttendanceResponse>>('/staff-attendances/realtime', {
      params: {
        unitId,
        start: range.start.formatIso(),
        end: range.end.formatIso()
      }
    })
    .then((res) => ({
      extraAttendances: res.data.extraAttendances.map(
        ({ arrived, departed, ...rest }) => ({
          ...rest,
          arrived: new Date(arrived),
          departed: departed ? new Date(departed) : null
        })
      ),
      staff: res.data.staff.map(({ attendances, ...rest }) => ({
        ...rest,
        attendances: attendances.map(({ arrived, departed, ...rest }) => ({
          ...rest,
          arrived: new Date(arrived),
          departed: departed ? new Date(departed) : null
        }))
      }))
    }))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}
