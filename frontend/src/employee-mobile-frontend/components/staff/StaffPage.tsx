/*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { useRestApi } from 'lib-common/utils/useRestApi'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Loader from 'lib-components/atoms/Loader'
import { ContentArea } from 'lib-components/layout/Container'
import { getDaycareAttendances, Group } from '../../api/attendances'
import { AttendanceUIContext } from '../../state/attendance-ui'
import TopBar from '../common/TopBar'
import BottomNavBar, { NavItem } from '../common/BottomNavbar'
import {
  getUnitStaffAttendances,
  postStaffAttendance
} from '../../api/staffAttendances'
import { getRealizedOccupancyToday } from '../../api/occupancy'
import {
  StaffAttendanceUpdate,
  UnitStaffAttendance
} from 'lib-common/api-types/staffAttendances'
import { combine, Loading, Result, Success } from 'lib-common/api'
import StaffAttendanceEditor from './StaffAttendanceEditor'
import LocalDate from '../../../lib-common/local-date'
import { staffAttendanceForGroupOrUnit } from '../../utils/staffAttendances'

export interface Props {
  onNavigate: (item: NavItem) => void
}

export default function StaffPage({ onNavigate }: Props) {
  const history = useHistory()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: string
    groupId: string
  }>()

  const { attendanceResponse, setAttendanceResponse } =
    useContext(AttendanceUIContext)
  const [staffAttendancesResponse, setStaffAttendancesResponse] = useState<
    Result<UnitStaffAttendance>
  >(Loading.of())
  const [occupancyResponse, setOccupancyResponse] = useState<Result<number>>(
    Loading.of()
  )

  const loadStaffAttendances = useRestApi(
    getUnitStaffAttendances,
    setStaffAttendancesResponse
  )
  const [selectedGroup, setSelectedGroup] = useState<Group | undefined>(
    undefined
  )
  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  const loadRealizedOccupancyToday = useRestApi(
    getRealizedOccupancyToday,
    setOccupancyResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
    loadStaffAttendances(unitId)
  }, [unitId, loadDaycareAttendances, loadStaffAttendances])

  useEffect(() => {
    if (
      attendanceResponse.isSuccess &&
      (selectedGroup === undefined ||
        (groupIdOrAll !== 'all' && selectedGroup.id !== groupIdOrAll))
    ) {
      setSelectedGroup(
        attendanceResponse.value.unit.groups.find(
          (group) => group.id === groupIdOrAll
        )
      )
    }
  }, [attendanceResponse, selectedGroup, groupIdOrAll])

  useEffect(() => {
    loadRealizedOccupancyToday(unitId, selectedGroup?.id)
  }, [unitId, selectedGroup, loadRealizedOccupancyToday])

  function changeGroup(group: Group | undefined) {
    const groupId = group === undefined ? 'all' : group.id
    history.push(`/units/${unitId}/staff/${groupId}`)
  }

  const updateAttendance = async (attendance: StaffAttendanceUpdate) => {
    await postStaffAttendance(attendance)
    const [attendances, occupancy] = await Promise.all([
      getUnitStaffAttendances(unitId),
      getRealizedOccupancyToday(unitId, selectedGroup?.id)
    ])
    setStaffAttendancesResponse(attendances)
    setOccupancyResponse(occupancy)
    return Success.of()
  }

  const today = LocalDate.today()

  return combine(
    attendanceResponse,
    staffAttendancesResponse.map((staffAttendances) =>
      staffAttendanceForGroupOrUnit(staffAttendances, selectedGroup?.id)
    ),
    occupancyResponse
  ).mapAll({
    failure() {
      return <ErrorSegment />
    },
    loading() {
      return <Loader />
    },
    success([attendance, staffAttendance, occupancy]) {
      return (
        <>
          <TopBar
            unitName={attendance.unit.name}
            selectedGroup={selectedGroup}
            onChangeGroup={changeGroup}
          />
          <ContentArea opaque fullHeight>
            <StaffAttendanceEditor
              groupId={selectedGroup?.id}
              date={today}
              count={staffAttendance.count}
              countOther={staffAttendance.countOther}
              updated={staffAttendance.updated}
              realizedOccupancy={occupancy}
              onConfirm={updateAttendance}
            />
          </ContentArea>
          <BottomNavBar
            selected="staff"
            staffCount={{
              count: staffAttendance.count ?? 0,
              countOther: staffAttendance.countOther ?? 0
            }}
            onChange={onNavigate}
          />
        </>
      )
    }
  })
}
