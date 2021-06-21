// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  StaffAttendanceUpdate,
  UnitStaffAttendance
} from 'lib-common/api-types/staffAttendances'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { client } from './client'

export async function getUnitStaffAttendances(
  unitId: UUID
): Promise<Result<UnitStaffAttendance>> {
  return client
    .get<JsonOf<UnitStaffAttendance>>(`/staff-attendances/unit/${unitId}`)
    .then((res) => res.data)
    .then((res) => ({
      ...res,
      date: LocalDate.parseIso(res.date),
      updated: res.updated ? new Date(res.updated) : null,
      groups: res.groups.map((group) => ({
        ...group,
        date: LocalDate.parseIso(group.date),
        updated: new Date(group.updated)
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postStaffAttendance(
  staffAttendance: StaffAttendanceUpdate
): Promise<Result<void>> {
  return client
    .post(
      `/staff-attendances/group/${staffAttendance.groupId}`,
      staffAttendance
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
