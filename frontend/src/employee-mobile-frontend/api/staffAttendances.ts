import { Failure, Response, Result, Success } from 'lib-common/api'
import {
  StaffAttendance,
  StaffAttendanceGroup
} from 'lib-common/api-types/staffAttendances'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { client } from './client'

interface SearchParams {
  year: number
  month: number
}

export async function getStaffAttendances(
  groupId: UUID,
  params: SearchParams
): Promise<Result<StaffAttendanceGroup>> {
  return client
    .get<JsonOf<Response<StaffAttendanceGroup>>>(
      `/staff-attendances/${groupId}`,
      { params }
    )
    .then((res) => res.data.data)
    .then((group) => ({
      ...group,
      startDate: LocalDate.parseIso(group.startDate),
      endDate: LocalDate.parseNullableIso(group.endDate),
      attendances: Object.entries(group.attendances).reduce(
        (attendanceMap, [key, attendance]) => ({
          ...attendanceMap,
          [key]: {
            ...attendance,
            date: LocalDate.parseIso(attendance.date)
          }
        }),
        {}
      )
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postStaffAttendance(
  staffAttendance: StaffAttendance
): Promise<Result<void>> {
  return client
    .post(`/staff-attendances/${staffAttendance.groupId}`, staffAttendance)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
