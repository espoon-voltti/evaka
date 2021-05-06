{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { useRestApi } from 'lib-common/utils/useRestApi'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Loader from 'lib-components/atoms/Loader'
import { ContentArea } from 'lib-components/layout/Container'
import { Group, getDaycareAttendances } from '../../api/attendances'
import { AttendanceUIContext } from '../../state/attendance-ui'
import TopBar from '../common/TopBar'
import BottomNavBar, { NavItem } from '../common/BottomNavbar'
import { getPagePath } from '../../App'
import {
  getStaffAttendances,
  postStaffAttendance
} from '../../api/staffAttendances'
import {
  OccupancyResponseGroupLevel,
  getRealizedOccupanciesToday
} from '../../api/occupancy'
import {
  StaffAttendance,
  StaffAttendanceGroup
} from 'lib-common/api-types/staffAttendances'
import LocalDate from 'lib-common/local-date'
import { Loading, Result, combine } from 'lib-common/api'
import StaffAttendanceEditor from './StaffAttendanceEditor'

export interface Props {
  onNavigate: (item: NavItem) => void
}

export default function StaffPage({ onNavigate }: Props) {
  const history = useHistory()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: string
    groupId: string
  }>()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )
  const [staffAttendancesResponse, setStaffAttendancesResponse] = useState<
    Result<StaffAttendanceGroup>
  >(Loading.of())
  const [occupancyResponse, setOccupancyResponse] = useState<
    Result<OccupancyResponseGroupLevel>
  >(Loading.of())
  const [saving, setSaving] = useState(false)

  const loadStaffAttendances = useRestApi(
    getStaffAttendances,
    setStaffAttendancesResponse
  )
  const [selectedGroup, setSelectedGroup] = useState<Group | undefined>(
    undefined
  )
  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )
  const today = LocalDate.today()

  const loadRealizedOccupanciesToday = useRestApi(
    getRealizedOccupanciesToday,
    setOccupancyResponse
  )
  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [unitId, loadDaycareAttendances])

  useEffect(() => {
    if (groupIdOrAll !== 'all') {
      loadStaffAttendances(groupIdOrAll, {
        year: today.year,
        month: today.month
      })
    }
  }, [groupIdOrAll, today.year, today.month, loadStaffAttendances])

  useEffect(() => {
    if (
      selectedGroup === undefined &&
      attendanceResponse.isSuccess &&
      attendanceResponse.value.unit.groups.length
    ) {
      if (groupIdOrAll === 'all') {
        const firstGroup = attendanceResponse.value.unit.groups[0]
        history.push(getPagePath('staff', { unitId, groupId: firstGroup.id }))
      }
    }
  }, [groupIdOrAll, attendanceResponse, history, selectedGroup, unitId])

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
    loadRealizedOccupanciesToday(unitId)
  }, [loadRealizedOccupanciesToday, unitId])

  function changeGroup(group: Group | undefined) {
    if (group !== undefined) {
      history.push(`/units/${unitId}/staff/${group.id}`)
    }
  }

  const updateAttendance = async (attendance: StaffAttendance) => {
    setSaving(true)
    await postStaffAttendance(attendance)
    const [attendances, occupancies] = await Promise.all([
      getStaffAttendances(groupIdOrAll, {
        year: today.year,
        month: today.month
      }),
      getRealizedOccupanciesToday(unitId)
    ])
    setStaffAttendancesResponse(attendances)
    setOccupancyResponse(occupancies)
    setSaving(false)
  }

  return combine(
    attendanceResponse,
    staffAttendancesResponse,
    occupancyResponse
  ).mapAll({
    failure() {
      return <ErrorSegment />
    },
    loading() {
      return <Loader />
    },
    success([attendance, staffAttendances, occupancy]) {
      const staffAttendance = staffAttendances.attendances[today.toString()]
      const groupOccupancy = occupancy.find(
        (group) => group.groupId === groupIdOrAll
      )?.occupancies.min?.percentage // There's only one occupancy, because we only fetch today's occupancy, so we can use `min`
      return (
        <>
          <TopBar
            unitName={attendance.unit.name}
            selectedGroup={selectedGroup}
            allowAllGroups={false}
            onChangeGroup={changeGroup}
          />
          <ContentArea opaque fullHeight>
            <StaffAttendanceEditor
              staffAttendance={staffAttendance}
              realizedOccupancy={groupOccupancy}
              isSaving={saving}
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
