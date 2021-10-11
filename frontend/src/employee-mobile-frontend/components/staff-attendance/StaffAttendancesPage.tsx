// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import Button from 'lib-components/atoms/buttons/Button'
import { faPlus } from 'lib-icons'
import { renderResult } from '../async-rendering'
import BottomNavBar from '../common/BottomNavbar'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { toStaff } from './staff'
import StaffListItem from './StaffListItem'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import TopBar from '../common/TopBar'
import { UnitContext } from '../../state/unit'
import { useHistory, useParams } from 'react-router-dom'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import Tabs from 'lib-components/molecules/Tabs'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

const Bold = styled.div`
  font-weight: ${fontWeights.semibold};
`
const StaticIconContainer = styled.div`
  position: fixed;
  bottom: 68px;
  right: 8px;
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
  const navigateToExternalMemberArrival = () =>
    history.push(`/units/${unitId}/groups/${groupId}/staff-attendance/external`)

  const presentStaffCounts = staffAttendanceResponse.map(
    (res) =>
      res.staff.filter((s) =>
        groupId === 'all' ? s.present : s.present === groupId
      ).length +
      res.extraAttendances.filter(
        (s) => groupId === 'all' || s.groupId === groupId
      ).length
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
          <br />({presentStaffCounts.getOrElse(' ')})
        </Bold>
      )
    }
  ]

  const filteredStaff = staffAttendanceResponse.map((res) =>
    showPresent
      ? groupId === 'all'
        ? [
            ...res.staff.filter((s) => s.present !== null),
            ...res.extraAttendances
          ]
        : [
            ...res.staff.filter((s) => s.present === groupId),
            ...res.extraAttendances.filter((s) => s.groupId === groupId)
          ]
      : res.staff.filter(
          (s) =>
            s.present === null &&
            (groupId === 'all' || s.groupIds.includes(groupId))
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
      <Tabs tabs={tabs} mobile type="buttons" />
      {renderResult(filteredStaff, (staff) => (
        <FixedSpaceColumn spacing="zero">
          {staff.map((staffMember) => {
            const s = toStaff(staffMember)
            return <StaffListItem {...s} key={s.id} />
          })}
        </FixedSpaceColumn>
      ))}
      <StaticIconContainer>
        <Button primary onClick={navigateToExternalMemberArrival}>
          <FontAwesomeIcon icon={faPlus} size="sm" /> Muu henkilö
        </Button>
      </StaticIconContainer>
      <BottomNavBar selected="staff" />
    </>
  ))
}
