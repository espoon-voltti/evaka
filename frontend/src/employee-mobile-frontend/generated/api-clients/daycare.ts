// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import { UUID } from 'lib-common/types'
import { UnitStaffAttendance } from 'lib-common/generated/api-types/daycare'
import { client } from '../../client'
import { deserializeJsonUnitStaffAttendance } from 'lib-common/generated/api-types/daycare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.daycare.controllers.StaffAttendanceController.getAttendancesByUnit
*/
export async function getAttendancesByUnit(
  request: {
    unitId: UUID
  }
): Promise<UnitStaffAttendance> {
  const { data: json } = await client.request<JsonOf<UnitStaffAttendance>>({
    url: uri`/staff-attendances/unit/${request.unitId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonUnitStaffAttendance(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.StaffAttendanceController.upsertStaffAttendance
*/
export async function upsertStaffAttendance(
  request: {
    groupId: UUID,
    body: StaffAttendanceUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/staff-attendances/group/${request.groupId}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<StaffAttendanceUpdate>
  })
  return json
}
