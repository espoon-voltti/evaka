// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import sortBy from 'lodash/sortBy'
import React, { Fragment, useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import styled, { useTheme } from 'styled-components'

import type { Result } from 'lib-common/api'
import type {
  GroupInfo,
  StaffAttendanceType
} from 'lib-common/generated/api-types/attendance'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import type LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { fontWeights } from 'lib-components/typography'
import { fasExclamationTriangle } from 'lib-icons'
import { faChevronDown, faChevronUp } from 'lib-icons'
import { faPlus } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'
import { toUnitOrGroup } from '../common/unit-or-group'
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
        <StaffAttendancesPlanned unitOrGroup={unitOrGroup} />
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

  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({ unitId: unitOrGroup.unitId })
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

interface StaffMemberDay {
  employeeId: EmployeeId
  firstName: string
  lastName: string
  occupancyEffect: boolean
  plans: {
    start: LocalTime | null // null if started on previous day
    end: LocalTime | null // null if ends on the next day
    type: StaffAttendanceType
    description: string | null
  }[]
  confidence: 'full' | 'maybeInOtherGroup' | 'maybeInOtherUnit'
}

interface StaffMembersByDate {
  date: LocalDate
  staff: StaffMemberDay[]
}

const StaffAttendancesPlanned = React.memo(function StaffAttendancesPlanned({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { i18n, lang } = useTranslation()
  const theme = useTheme()
  const today = LocalDate.todayInHelsinkiTz()

  const [expandedDate, setExpandedDate] = useState<LocalDate | null>(null)

  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({
      unitId: unitOrGroup.unitId,
      startDate: today,
      endDate: today.addDays(5)
    })
  )

  const staffMemberDays: Result<StaffMembersByDate[]> = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        [1, 2, 3, 4, 5].map((i) => {
          const date = today.addDays(i)
          return {
            date,
            staff: res.staff
              .filter(
                (s) =>
                  unitOrGroup.type !== 'group' ||
                  s.groupIds.includes(unitOrGroup.id)
              )
              .map((s) => ({
                employeeId: s.employeeId,
                firstName: s.firstName,
                lastName: s.lastName,
                occupancyEffect: s.occupancyEffect,
                plans: s.plannedAttendances
                  .filter(
                    (p) =>
                      p.start.toLocalDate().isEqual(date) ||
                      p.end.toLocalDate().isEqual(date)
                  )
                  .map((p) => ({
                    start: p.start.toLocalDate().isEqual(date)
                      ? p.start.toLocalTime()
                      : null,
                    end: p.end.toLocalDate().isEqual(date)
                      ? p.end.toLocalTime()
                      : null,
                    type: p.type,
                    description: p.description
                  })),
                confidence:
                  s.unitIds.length > 1
                    ? 'maybeInOtherUnit'
                    : s.groupIds.length > 1
                      ? 'maybeInOtherGroup'
                      : 'full'
              }))
          }
        })
      ),
    [unitOrGroup, staffAttendanceResponse, today]
  )

  return renderResult(staffMemberDays, (days) => (
    <FixedSpaceColumn spacing="xxs">
      <DayRow $open={false}>
        <InfoBox message={i18n.attendances.staff.plansInfo} noMargin thin />
      </DayRow>
      {days.map(({ date, staff }) => (
        <Fragment key={date.formatIso()}>
          <DayRow
            data-qa={`date-row-${date.formatIso()}`}
            onClick={() =>
              setExpandedDate(expandedDate?.isEqual(date) ? null : date)
            }
            justifyContent="space-between"
            alignItems="center"
            $open={expandedDate?.isEqual(date) ?? false}
          >
            <div>{date.formatExotic('EEEEEE d.M.', lang)}</div>
            <div>
              <FontAwesomeIcon
                icon={expandedDate?.isEqual(date) ? faChevronUp : faChevronDown}
                color={theme.colors.main.m2}
                style={{ fontSize: '24px' }}
              />
            </div>
          </DayRow>
          {expandedDate?.isEqual(date) && (
            <ExpandedStaff
              spacing="L"
              data-qa={`expanded-date-${date.formatIso()}`}
            >
              {sortBy(
                staff.filter((s) => s.plans.length > 0),
                (s) => s.firstName,
                (s) => s.lastName
              ).map((s) => (
                <FixedSpaceColumn
                  key={s.employeeId}
                  spacing="xs"
                  data-qa={`present-employee-${s.employeeId}`}
                >
                  <FixedSpaceRow spacing="zero" alignItems="center">
                    <IconWrapper>
                      {s.occupancyEffect && (
                        <RoundIcon
                          content="K"
                          active={true}
                          color={theme.colors.accents.a3emerald}
                          size="s"
                        />
                      )}
                    </IconWrapper>
                    <StaffName>{`${s.firstName} ${s.lastName}`}</StaffName>
                  </FixedSpaceRow>

                  {s.confidence !== 'full' && (
                    <FixedSpaceRow spacing="zero" alignItems="center">
                      <IconWrapper>
                        <FontAwesomeIcon
                          icon={fasExclamationTriangle}
                          color={theme.colors.status.warning}
                        />
                      </IconWrapper>
                      <span data-qa="confidence-warning">
                        {i18n.attendances.staff.planWarnings[s.confidence]}
                      </span>
                    </FixedSpaceRow>
                  )}

                  {s.plans.map((p, i) => (
                    <DetailsRow key={i} alignItems="start">
                      <PlanType>{i18n.attendances.staffTypes[p.type]}</PlanType>
                      <div>
                        <div>{`${p.start?.format() ?? '→'} - ${p.end?.format() ?? '→'}`}</div>
                        {p.description ? <i>({p.description})</i> : null}
                      </div>
                    </DetailsRow>
                  ))}
                </FixedSpaceColumn>
              ))}

              {staff.some((s) => s.plans.length > 0) &&
                staff.some((s) => s.plans.length === 0) && <NoPlansSeparator />}

              {sortBy(
                staff.filter((s) => s.plans.length === 0),
                (s) => s.firstName,
                (s) => s.lastName
              ).map((s) => (
                <AbsentStaff
                  key={s.employeeId}
                  data-qa={`absent-employee-${s.employeeId}`}
                >
                  <FixedSpaceColumn spacing="xxs">
                    <FixedSpaceRow spacing="zero" alignItems="center">
                      <IconWrapper>
                        {s.occupancyEffect && (
                          <RoundIcon
                            content="K"
                            active={true}
                            color={theme.colors.accents.a3emerald}
                            size="s"
                          />
                        )}
                      </IconWrapper>
                      <StaffName>{`${s.firstName} ${s.lastName}`}</StaffName>
                    </FixedSpaceRow>
                    <DetailsRow>{i18n.attendances.staff.noPlan}</DetailsRow>
                  </FixedSpaceColumn>
                </AbsentStaff>
              ))}
            </ExpandedStaff>
          )}
        </Fragment>
      ))}
    </FixedSpaceColumn>
  ))
})

const DayRow = styled(FixedSpaceRow)<{ $open: boolean }>`
  border-left: 4px solid
    ${(p) => (p.$open ? p.theme.colors.main.m2 : 'transparent')};
  padding: 16px;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const ExpandedStaff = styled(FixedSpaceColumn)`
  padding: 16px 8px;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const AbsentStaff = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const StaffName = styled.div`
  font-weight: ${fontWeights.semibold};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const PlanType = styled.div`
  width: 30%;
`

const IconWrapper = styled.div`
  min-width: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
`

const DetailsRow = styled(FixedSpaceRow)`
  padding-left: 40px;
`

const NoPlansSeparator = styled(HorizontalLine)`
  margin-block-start: 8px;
  margin-block-end: 16px;
`
