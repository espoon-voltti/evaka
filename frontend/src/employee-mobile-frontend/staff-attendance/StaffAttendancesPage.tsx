// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { faPlus } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup, toUnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import StaffListItem from './StaffListItem'
import { staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

const StaticIconContainer = styled.div`
  position: fixed;
  bottom: 68px;
  right: 8px;
`

type PrimaryTab = 'today' | 'planned'
type StatusTab = 'present' | 'absent'

type Props = {
  unitOrGroup: UnitOrGroup
  primaryTab: PrimaryTab
} & (
  | {
      primaryTab: 'today'
      statusTab: StatusTab
    }
  | {
      primaryTab: 'planned'
    }
)

export default React.memo(function StaffAttendancesPage(props: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const unitOrGroup = props.unitOrGroup
  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      void navigate(
        props.primaryTab === 'today'
          ? routes.staffAttendancesToday(
              toUnitOrGroup(unitId, group?.id),
              props.statusTab
            ).value
          : routes.staffAttendancesPlanned(toUnitOrGroup(unitId, group?.id))
              .value
      )
    },
    [navigate, props, unitId]
  )

  const selectedGroup = useMemo(
    () =>
      unitInfoResponse
        .map(({ groups }) =>
          unitOrGroup.type === 'unit'
            ? undefined
            : groups.find((g) => g.id === unitOrGroup.id)
        )
        .getOrElse(undefined),
    [unitOrGroup, unitInfoResponse]
  )

  const tabs = useMemo(
    () => [
      {
        id: 'today',
        link: routes.staffAttendancesToday(unitOrGroup, 'absent'),
        label: i18n.attendances.views.TODAY
      },
      {
        id: 'planned',
        link: routes.staffAttendancesPlanned(unitOrGroup),
        label: i18n.attendances.views.NEXT_DAYS
      }
    ],
    [unitOrGroup, i18n]
  )

  return (
    <PageWithNavigation
      unitOrGroup={unitOrGroup}
      selected="staff"
      selectedGroup={selectedGroup}
      onChangeGroup={changeGroup}
    >
      <TabLinks tabs={tabs} mobile />
      {props.primaryTab === 'today' ? (
        <StaffAttendancesToday
          unitOrGroup={unitOrGroup}
          tab={props.statusTab}
        />
      ) : (
        <div>todo</div>
      )}
    </PageWithNavigation>
  )
})

const StaffAttendancesToday = React.memo(function StaffAttendancesToday({
  unitOrGroup,
  tab
}: {
  unitOrGroup: UnitOrGroup
  tab: StatusTab
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const unitId = unitOrGroup.unitId

  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({ unitId })
  )

  const navigateToExternalMemberArrival = useCallback(
    () => navigate(routes.externalStaffAttendances(unitOrGroup).value),
    [unitOrGroup, navigate]
  )

  const presentStaffCounts = useMemo(
    () =>
      staffAttendanceResponse.map(
        (res) =>
          res.staff.filter((s) =>
            unitOrGroup.type === 'unit'
              ? s.present
              : s.present === unitOrGroup.id
          ).length +
          res.extraAttendances.filter(
            (s) => unitOrGroup.type === 'unit' || s.groupId === unitOrGroup.id
          ).length
      ),
    [unitOrGroup, staffAttendanceResponse]
  )

  const tabs = useMemo(
    () => [
      {
        id: 'absent',
        link: routes.staffAttendancesToday(unitOrGroup, 'absent'),
        label: i18n.attendances.types.ABSENT
      },
      {
        id: 'present',
        link: routes.staffAttendancesToday(unitOrGroup, 'present'),
        label: (
          <>
            {i18n.attendances.types.PRESENT}
            <br />({presentStaffCounts.getOrElse('0')})
          </>
        )
      }
    ],
    [unitOrGroup, i18n, presentStaffCounts]
  )

  const filteredStaff = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        tab === 'present'
          ? unitOrGroup.type === 'unit'
            ? [
                ...res.staff.filter((s) => s.present !== null),
                ...res.extraAttendances
              ]
            : [
                ...res.staff.filter((s) => s.present === unitOrGroup.id),
                ...res.extraAttendances.filter(
                  (s) => s.groupId === unitOrGroup.id
                )
              ]
          : res.staff.filter(
              (s) =>
                s.present === null &&
                (unitOrGroup.type === 'unit' ||
                  s.groupIds.includes(unitOrGroup.id))
            )
      ),
    [unitOrGroup, tab, staffAttendanceResponse]
  )

  return (
    <>
      <TabLinks tabs={tabs} mobile />
      {renderResult(filteredStaff, (staff) => (
        <FixedSpaceColumn spacing="zero">
          {staff.map((staffMember) => {
            const s = toStaff(staffMember)
            return (
              <StaffListItem
                {...s}
                key={s.id}
                unitOrGroup={unitOrGroup}
                occupancyEffect={staffMember.occupancyEffect}
              />
            )
          })}
        </FixedSpaceColumn>
      ))}
      <StaticIconContainer>
        <LegacyButton
          primary
          onClick={navigateToExternalMemberArrival}
          data-qa="add-external-member-btn"
        >
          <FontAwesomeIcon icon={faPlus} size="sm" />{' '}
          {i18n.attendances.staff.externalPerson}
        </LegacyButton>
      </StaticIconContainer>
    </>
  )
})
