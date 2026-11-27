// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faEdit } from '@fortawesome/free-solid-svg-icons'
import sortBy from 'lodash/sortBy'
import React, { useMemo } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import type FiniteDateTimeRange from 'lib-common/finite-date-time-range'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { isStaffAttendanceTypesEnabled } from 'lib-customizations/employeeMobile'
import { faCalendar } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { TimeInfo } from './components/staff-components'
import { openAttendanceQuery, staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

export default React.memo(function StaffMemberPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const employeeId = useIdRouteParam<EmployeeId>('employeeId')
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()

  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({ unitId })
  )

  const employeeResponse = useMemo(
    () =>
      combine(unitInfoResponse, staffAttendanceResponse).map(
        ([{ isOperationalDate }, { staff }]) => ({
          isOperationalDate,
          staffMember: staff.find((s) => s.employeeId === employeeId)
        })
      ),
    [employeeId, unitInfoResponse, staffAttendanceResponse]
  )

  const openAttendanceResult = useQueryResult(
    employeeId
      ? openAttendanceQuery({ userId: employeeId })
      : constantQuery({ openGroupAttendance: null })
  )
  const openAttendance = openAttendanceResult.isSuccess
    ? openAttendanceResult.value.openGroupAttendance
    : null

  const openAttendanceInAnotherUnit =
    !!openAttendance && openAttendance.unitId !== unitId

  function formatRange(range: FiniteDateTimeRange) {
    const times = range.timesOn(LocalDate.todayInHelsinkiTz())
    const start = times.start?.format() ?? range.start.format('d.M. HH:mm')
    const end = times.end?.format() ?? range.end.format('d.M. HH:mm')
    return `${start} – ${end}`
  }

  const spanningPlans = useMemo(() => {
    function getInfo(range: FiniteDateTimeRange) {
      const plans = employeeResponse.isSuccess
        ? sortBy(
            employeeResponse.value.staffMember?.plannedAttendances.filter(
              (plan) => range.contains(plan.period)
            ) ?? [],
            (a) => a.period.start
          )
        : []
      if (
        plans.length > 1 ||
        plans.some((plan) => plan.type !== 'PRESENT') ||
        plans.some((plan) => plan.description)
      )
        return (
          <InfoListContainer>
            {plans.map((plan) => (
              <li key={plan.period.start.formatIso()}>
                <div>
                  <div>
                    {`${i18n.attendances.staffTypes[plan.type]} ${formatRange(plan.period)}`}
                  </div>
                  {plan.description ? <i>({plan.description})</i> : null}
                </div>
              </li>
            ))}
          </InfoListContainer>
        )
      return null
    }

    return employeeResponse.isSuccess
      ? (employeeResponse.value.staffMember?.spanningPlans.map((span) => ({
          span,
          info: getInfo(span)
        })) ?? [])
      : []
  }, [employeeResponse, i18n])

  return renderResult(
    employeeResponse,
    ({ isOperationalDate, staffMember }) => (
      <StaffMemberPageContainer
        back={routes.staffAttendancesToday(unitOrGroup).value}
      >
        {staffMember === undefined ? (
          <ErrorSegment
            title={i18n.attendances.staff.errors.employeeNotFound}
          />
        ) : (
          <>
            <EmployeeCardBackground staff={toStaff(staffMember)} />
            <Gap $size="s" />
            <FixedSpaceColumn>
              {isStaffAttendanceTypesEnabled ? (
                <>
                  {spanningPlans.length > 0 && (
                    <TimeInfo data-qa="shift-time">
                      <span>{i18n.attendances.staff.plannedAttendance}</span>
                      <PlanContainer>
                        {spanningPlans.map(({ span, info }, i) => (
                          <ExpandingInfo
                            info={info}
                            key={`plan-${i}`}
                            inlineChildren
                            data-qa="shift-info"
                          >
                            <span>{formatRange(span)}</span>
                          </ExpandingInfo>
                        ))}
                      </PlanContainer>
                    </TimeInfo>
                  )}
                  {staffMember.attendances.length > 0 &&
                    sortBy(staffMember.attendances, (a) => a.period.start).map(
                      ({ period, type }, index, attendances) => (
                        <TimeInfo
                          key={period.start.formatIso()}
                          data-qa="attendance-time"
                        >
                          <Label>{i18n.attendances.staffTypes[type]}</Label>{' '}
                          <span>
                            {period.start.toLocalTime().format()}–
                            {period.end?.toLocalTime().format() ?? ''}
                            {index === attendances.length - 1 && (
                              <InlineIconButton
                                icon={faEdit}
                                onClick={() =>
                                  navigate(
                                    routes.staffAttendanceEdit(
                                      unitOrGroup,
                                      staffMember.employeeId
                                    ).value
                                  )
                                }
                                aria-label={i18n.common.edit}
                                data-qa="edit"
                              />
                            )}
                          </span>
                        </TimeInfo>
                      )
                    )}
                </>
              ) : (
                staffMember.latestCurrentDayAttendance && (
                  <>
                    <TimeInfo>
                      <Label>{i18n.attendances.arrivalTime}</Label>{' '}
                      <span data-qa="arrival-time">
                        {staffMember.latestCurrentDayAttendance.period.start
                          .toLocalTime()
                          .format()}
                      </span>
                    </TimeInfo>
                    <TimeInfo>
                      {staffMember.latestCurrentDayAttendance.period.end && (
                        <>
                          <Label>{i18n.attendances.departureTime}</Label>{' '}
                          <span data-qa="departure-time">
                            {staffMember.latestCurrentDayAttendance.period.end
                              ?.toLocalTime()
                              .format()}
                          </span>
                        </>
                      )}
                      <InlineIconButton
                        icon={faEdit}
                        onClick={() =>
                          navigate(
                            routes.staffAttendanceEdit(
                              unitOrGroup,
                              staffMember.employeeId
                            ).value
                          )
                        }
                        aria-label={i18n.common.edit}
                        data-qa="edit"
                      />
                    </TimeInfo>
                  </>
                )
              )}
              <ContentArea $opaque $paddingHorizontal="s">
                {openAttendanceInAnotherUnit && (
                  <AlertBox
                    data-qa="open-attendance-in-another-unit-warning"
                    message={`${i18n.attendances.staff.openAttendanceInAnotherUnitWarning} ${
                      openAttendance.date.formatExotic('EEEEEE d.M.yyyy') +
                      ' - ' +
                      openAttendance.unitName
                    }${i18n.attendances.staff.openAttendanceInAnotherUnitWarningCont}`}
                  />
                )}
                <FixedSpaceColumn $alignItems="center">
                  {staffMember.present ? (
                    <Button
                      primary
                      data-qa="mark-departed-btn"
                      onClick={() =>
                        navigate(
                          routes.staffMarkDeparted(
                            unitOrGroup,
                            staffMember.employeeId
                          ).value
                        )
                      }
                      text={i18n.attendances.staff.markDeparted}
                    />
                  ) : (
                    <>
                      {!isOperationalDate && (
                        <H4 $centered={true}>
                          {i18n.attendances.notOperationalDate}
                        </H4>
                      )}
                      <Button
                        primary
                        data-qa="mark-arrived-btn"
                        disabled={
                          !isOperationalDate || openAttendanceInAnotherUnit
                        }
                        onClick={() =>
                          navigate(
                            routes.staffMarkArrived(
                              unitOrGroup,
                              staffMember.employeeId
                            ).value
                          )
                        }
                        text={i18n.attendances.staff.markArrived}
                      />
                    </>
                  )}
                  <Button
                    data-qa="previous-attendances"
                    appearance="inline"
                    text={i18n.attendances.staff.previousDays}
                    onClick={() =>
                      navigate(
                        routes.staffPreviousAttendances(
                          unitOrGroup,
                          staffMember.employeeId
                        ).value
                      )
                    }
                  />
                  <Button
                    data-qa="planned-attendances"
                    appearance="inline"
                    text={i18n.attendances.staff.nextDays}
                    icon={faCalendar}
                    onClick={() =>
                      navigate(
                        routes.staffPlannedAttendances(
                          unitOrGroup,
                          staffMember.employeeId
                        ).value
                      )
                    }
                  />
                </FixedSpaceColumn>
              </ContentArea>
            </FixedSpaceColumn>
          </>
        )}
      </StaffMemberPageContainer>
    )
  )
})

const InlineIconButton = styled(IconOnlyButton)`
  display: inline-flex;
  margin-left: ${defaultMargins.xxs};
`
const PlanContainer = styled.div`
  padding-inline: ${defaultMargins.s};
`
const InfoListContainer = styled.ul`
  width: fit-content;
  text-align: left;
  margin: 0 auto;
`
