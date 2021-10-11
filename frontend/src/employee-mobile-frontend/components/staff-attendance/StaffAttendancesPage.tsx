// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { renderResult } from '../async-rendering'
import BottomNavBar from '../common/BottomNavbar'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import StaffListItem from './StaffListItem'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import TopBar from '../common/TopBar'
import { UnitContext } from '../../state/unit'
import { useHistory, useParams } from 'react-router-dom'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import Tabs from 'lib-components/molecules/Tabs'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'

const Bold = styled.div`
  font-weight: ${fontWeights.semibold};
`

export default function StaffAttendancesPage() {
  const history = useHistory()
  const { unitId, groupId } = useParams<{
    unitId: string
    groupId: string
  }>()

  const { unitInfoResponse, showPresent, setShowPresent } =
    useContext(UnitContext)
  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )
  useEffect(reloadStaffAttendance, [reloadStaffAttendance])

  const changeGroup = (group: GroupInfo | undefined) =>
    history.push(
      `/units/${unitId}/groups/${group?.id ?? 'all'}/staff-attendance`
    )

  const presentStaffCounts = staffAttendanceResponse.map(
    (res) =>
      res.staff.filter((s) =>
        groupId === 'all' ? s.present : s.present === groupId
      ).length +
      res.extraAttendances.filter((s) => s.groupId === groupId).length
  )

  const tabs = [
    {
      id: 'not-present',
      onClick: () => setShowPresent(false),
      active: !showPresent,
      label: <Bold>Poissa</Bold>
    },
    {
      id: 'present',
      onClick: () => setShowPresent(true),
      active: showPresent,
      label: (
        <Bold>
          Läsnä
          <br />
          {`(${presentStaffCounts.getOrElse(' ')})`}
        </Bold>
      )
    }
  ]

  const filteredStaff = staffAttendanceResponse.map((res) =>
    showPresent
      ? groupId === 'all'
        ? [
            ...res.staff.filter((s) => s.present !== null)
            // todo: extra attendances
          ]
        : [
            ...res.staff.filter((s) => s.present === groupId)
            // todo: extra attendances
          ]
      : groupId === 'all'
      ? res.staff.filter((s) => s.present === null)
      : res.staff.filter(
          (s) => s.groupIds.includes(groupId) && s.present === null
        )
  )

  return renderResult(unitInfoResponse, (unit) => (
    <>
      <TopBar
        unitName={unit.name}
        selectedGroup={
          groupId === 'all'
            ? undefined
            : unit.groups.find((g) => g.id === groupId)
        }
        onChangeGroup={changeGroup}
      />
      <Tabs tabs={tabs} mobile type={'buttons'} />
      {renderResult(filteredStaff, (filteredStaff) => (
        <FixedSpaceColumn spacing="zero">
          {filteredStaff.map((staffMember) => (
            <StaffListItem
              staffMember={staffMember}
              key={staffMember.employeeId}
            />
          ))}
        </FixedSpaceColumn>
      ))}
      <BottomNavBar selected="staff" />
    </>
  ))
}
