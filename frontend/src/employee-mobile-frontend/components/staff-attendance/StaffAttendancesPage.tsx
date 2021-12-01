// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Tabs from 'lib-components/molecules/Tabs'
import { fontWeights } from 'lib-components/typography'
import { faPlus } from 'lib-icons'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import BottomNavBar from '../common/BottomNavbar'
import TopBarWithGroupSelector from '../common/TopBarWithGroupSelector'
import { toStaff } from './staff'
import StaffListItem from './StaffListItem'

const Bold = styled.div`
  font-weight: ${fontWeights.semibold};
`
const StaticIconContainer = styled.div`
  position: fixed;
  bottom: 68px;
  right: 8px;
`

export default React.memo(function StaffAttendancesPage() {
  const history = useHistory()
  const { unitId, groupId } = useParams<{
    unitId: string
    groupId: string
  }>()
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)
  const { staffAttendanceResponse } = useContext(StaffAttendanceContext)
  const [showPresent, setShowPresent] = useState(false)

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      history.push(
        `/units/${unitId}/groups/${group?.id ?? 'all'}/staff-attendance`
      )
    },
    [history, unitId]
  )

  const navigateToExternalMemberArrival = useCallback(
    () =>
      history.push(
        `/units/${unitId}/groups/${groupId}/staff-attendance/external`
      ),
    [groupId, history, unitId]
  )

  const presentStaffCounts = useMemo(
    () =>
      staffAttendanceResponse.map(
        (res) =>
          res.staff.filter((s) =>
            groupId === 'all' ? s.present : s.present === groupId
          ).length +
          res.extraAttendances.filter(
            (s) => groupId === 'all' || s.groupId === groupId
          ).length
      ),
    [groupId, staffAttendanceResponse]
  )

  const tabs = useMemo(
    () => [
      {
        id: 'not-present',
        onClick: () => setShowPresent(false),
        active: !showPresent,
        label: <Bold>{i18n.attendances.types.ABSENT}</Bold>
      },
      {
        id: 'present',
        onClick: () => setShowPresent(true),
        active: showPresent,
        label: (
          <Bold>
            {i18n.attendances.types.PRESENT}
            <br />({presentStaffCounts.getOrElse(' ')})
          </Bold>
        )
      }
    ],
    [i18n.attendances.types, presentStaffCounts, setShowPresent, showPresent]
  )

  const filteredStaff = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
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
      ),
    [groupId, showPresent, staffAttendanceResponse]
  )

  const selectedGroup = useMemo(
    () =>
      unitInfoResponse
        .map(({ groups }) =>
          groupId === 'all' ? undefined : groups.find((g) => g.id === groupId)
        )
        .getOrElse(undefined),
    [groupId, unitInfoResponse]
  )

  return (
    <>
      <TopBarWithGroupSelector
        selectedGroup={selectedGroup}
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
          <FontAwesomeIcon icon={faPlus} size="sm" />{' '}
          {i18n.attendances.staff.externalPerson}
        </Button>
      </StaticIconContainer>
      <BottomNavBar selected="staff" />
    </>
  )
})
