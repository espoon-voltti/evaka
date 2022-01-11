// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useMemo } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Tabs from 'lib-components/molecules/Tabs'
import { faPlus } from 'lib-icons'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import { PageWithNavigation } from '../common/PageWithNavigation'
import StaffListItem from './StaffListItem'
import { toStaff } from './staff'

const StaticIconContainer = styled.div`
  position: fixed;
  bottom: 68px;
  right: 8px;
`

type StatusTab = 'present' | 'absent'

export default React.memo(function StaffAttendancesPage() {
  const history = useHistory()
  const { unitId, groupId, tab } = useParams<{
    unitId: string
    groupId: string
    tab: StatusTab
  }>()
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)
  const { staffAttendanceResponse } = useContext(StaffAttendanceContext)

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      history.push(
        `/units/${unitId}/groups/${group?.id ?? 'all'}/staff-attendance/${tab}`
      )
    },
    [history, tab, unitId]
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
        link: `/units/${unitId}/groups/${groupId}/staff-attendance/absent`,
        label: i18n.attendances.types.ABSENT
      },
      {
        id: 'present',
        link: `/units/${unitId}/groups/${groupId}/staff-attendance/present`,
        label: (
          <>
            {i18n.attendances.types.PRESENT}
            <br />({presentStaffCounts.getOrElse('0')})
          </>
        )
      }
    ],
    [groupId, i18n, presentStaffCounts, unitId]
  )

  const filteredStaff = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        tab === 'present'
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
    [groupId, tab, staffAttendanceResponse]
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
    <PageWithNavigation
      selected="staff"
      selectedGroup={selectedGroup}
      onChangeGroup={changeGroup}
    >
      <Tabs tabs={tabs} mobile />
      {renderResult(filteredStaff, (staff) => (
        <FixedSpaceColumn spacing="zero">
          {staff.map((staffMember) => {
            const s = toStaff(staffMember)
            return <StaffListItem {...s} key={s.id} />
          })}
        </FixedSpaceColumn>
      ))}
      <StaticIconContainer>
        <Button
          primary
          onClick={navigateToExternalMemberArrival}
          data-qa="add-external-member-btn"
        >
          <FontAwesomeIcon icon={faPlus} size="sm" />{' '}
          {i18n.attendances.staff.externalPerson}
        </Button>
      </StaticIconContainer>
    </PageWithNavigation>
  )
})
