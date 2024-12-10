// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { DaycareId } from 'lib-common/generated/api-types/shared'
import { GroupId } from 'lib-common/generated/api-types/shared'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import { UnitStaffAttendance } from 'lib-common/generated/api-types/daycare'
import { client } from '../../client'
import { deserializeJsonUnitStaffAttendance } from 'lib-common/generated/api-types/daycare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.daycare.controllers.StaffAttendanceController.getAttendancesByUnit
*/
export async function getAttendancesByUnit(
  request: {
    unitId: DaycareId
  }
): Promise<UnitStaffAttendance> {
  const { data: json } = await client.request<JsonOf<UnitStaffAttendance>>({
    url: uri`/employee-mobile/staff-attendances/unit/${request.unitId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonUnitStaffAttendance(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.StaffAttendanceController.upsertStaffAttendance
*/
export async function upsertStaffAttendance(
  request: {
    groupId: GroupId,
    body: StaffAttendanceUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/staff-attendances/group/${request.groupId}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<StaffAttendanceUpdate>
  })
  return json
}
