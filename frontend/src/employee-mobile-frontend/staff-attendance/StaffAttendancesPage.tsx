// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { faPlus } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { unitInfoQuery } from '../units/queries'

import StaffListItem from './StaffListItem'
import { staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

const StaticIconContainer = styled.div`
  position: fixed;
  bottom: 68px;
  right: 8px;
`

type StatusTab = 'present' | 'absent'

interface Props {
  tab: StatusTab
}

export default React.memo(function StaffAttendancesPage({ tab }: Props) {
  const navigate = useNavigate()
  const { unitId } = useRouteParams(['unitId'])
  const { selectedGroupId, groupRoute } = useSelectedGroup()
  const { i18n } = useTranslation()
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const staffAttendanceResponse = useQueryResult(staffAttendanceQuery(unitId))

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      navigate(
        `/units/${unitId}/groups/${group?.id ?? 'all'}/staff-attendance/${tab}`
      )
    },
    [navigate, tab, unitId]
  )

  const navigateToExternalMemberArrival = useCallback(
    () => navigate(`${groupRoute}/staff-attendance/external`),
    [groupRoute, navigate]
  )

  const presentStaffCounts = useMemo(
    () =>
      staffAttendanceResponse.map(
        (res) =>
          res.staff.filter((s) =>
            selectedGroupId.type === 'all'
              ? s.present
              : s.present === selectedGroupId.id
          ).length +
          res.extraAttendances.filter(
            (s) =>
              selectedGroupId.type === 'all' || s.groupId === selectedGroupId.id
          ).length
      ),
    [selectedGroupId, staffAttendanceResponse]
  )

  const tabs = useMemo(
    () => [
      {
        id: 'absent',
        link: `${groupRoute}/staff-attendance/absent`,
        label: i18n.attendances.types.ABSENT
      },
      {
        id: 'present',
        link: `${groupRoute}/staff-attendance/present`,
        label: (
          <>
            {i18n.attendances.types.PRESENT}
            <br />({presentStaffCounts.getOrElse('0')})
          </>
        )
      }
    ],
    [groupRoute, i18n, presentStaffCounts]
  )

  const filteredStaff = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        tab === 'present'
          ? selectedGroupId.type === 'all'
            ? [
                ...res.staff.filter((s) => s.present !== null),
                ...res.extraAttendances
              ]
            : [
                ...res.staff.filter((s) => s.present === selectedGroupId.id),
                ...res.extraAttendances.filter(
                  (s) => s.groupId === selectedGroupId.id
                )
              ]
          : res.staff.filter(
              (s) =>
                s.present === null &&
                (selectedGroupId.type === 'all' ||
                  s.groupIds.includes(selectedGroupId.id))
            )
      ),
    [selectedGroupId, tab, staffAttendanceResponse]
  )

  const selectedGroup = useMemo(
    () =>
      unitInfoResponse
        .map(({ groups }) =>
          selectedGroupId.type === 'all'
            ? undefined
            : groups.find((g) => g.id === selectedGroupId.id)
        )
        .getOrElse(undefined),
    [selectedGroupId, unitInfoResponse]
  )

  return (
    <PageWithNavigation
      selected="staff"
      selectedGroup={selectedGroup}
      onChangeGroup={changeGroup}
    >
      <TabLinks tabs={tabs} mobile />
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
