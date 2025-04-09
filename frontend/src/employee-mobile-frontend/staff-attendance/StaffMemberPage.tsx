// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faEdit } from '@fortawesome/free-solid-svg-icons'
import orderBy from 'lodash/orderBy'
import React, { useMemo } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  EmployeeId,
  HelsinkiDateTimeRange
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faCalendar } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup } from '../common/unit-or-group'
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
  const navigate = useNavigate()

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

  function formatTime(time: HelsinkiDateTime) {
    const today = LocalDate.todayInHelsinkiTz()
    return time.toLocalDate().isEqual(today)
      ? time.toLocalTime().format()
      : time.format('d.M. HH:mm')
  }

  const spanningPlans = useMemo(() => {
    function getInfo(range: HelsinkiDateTimeRange) {
      const plans = employeeResponse.isSuccess
        ? (
            employeeResponse.value.staffMember?.plannedAttendances.filter(
              (plan) =>
                plan.start.isEqualOrAfter(range.start) &&
                plan.end.isEqualOrBefore(range.end)
            ) ?? []
          ).sort((a, b) => a.start.timestamp - b.start.timestamp)
        : []
      if (
        plans.length > 1 ||
        plans.some((plan) => plan.type !== 'PRESENT') ||
        plans.some((plan) => plan.description)
      )
        return (
          <InfoListContainer>
            {plans.map((plan) => (
              <li key={plan.start.formatIso()}>
                <div>
                  <div>
                    {`${i18n.attendances.staffTypes[plan.type]} ${formatTime(plan.start)} – ${formatTime(plan.end)}`}
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
        back={routes.staffAttendancesToday(unitOrGroup, 'present').value}
      >
        {staffMember === undefined ? (
          <ErrorSegment
            title={i18n.attendances.staff.errors.employeeNotFound}
          />
        ) : (
          <>
            <EmployeeCardBackground staff={toStaff(staffMember)} />
            <FixedSpaceColumn>
              {featureFlags.staffAttendanceTypes ? (
                <>
                  {spanningPlans.length > 0 && (
                    <TimeInfo data-qa="shift-time">
                      <span>{i18n.attendances.staff.plannedAttendance}</span>
                      <PlanContainer>
                        {spanningPlans.map(({ span, info }, i) => {
                          const start = formatTime(span.start)
                          const end = formatTime(span.end)
                          return (
                            <ExpandingInfo
                              info={info}
                              key={`plan-${i}`}
                              inlineChildren
                            >
                              <span>{`${start} – ${end}`}</span>
                            </ExpandingInfo>
                          )
                        })}
                      </PlanContainer>
                    </TimeInfo>
                  )}
                  {staffMember.attendances.length > 0 &&
                    orderBy(staffMember.attendances, ({ arrived }) =>
                      arrived.formatIso()
                    ).map(({ arrived, departed, type }, index, attendances) => (
                      <TimeInfo
                        key={arrived.formatIso()}
                        data-qa="attendance-time"
                      >
                        <Label>{i18n.attendances.staffTypes[type]}</Label>{' '}
                        <span>
                          {arrived.toLocalTime().format()}–
                          {departed?.toLocalTime().format() ?? ''}
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
                    ))}
                </>
              ) : (
                staffMember.latestCurrentDayAttendance && (
                  <>
                    <TimeInfo>
                      <Label>{i18n.attendances.arrivalTime}</Label>{' '}
                      <span data-qa="arrival-time">
                        {staffMember.latestCurrentDayAttendance.arrived
                          .toLocalTime()
                          .format()}
                      </span>
                    </TimeInfo>
                    <TimeInfo>
                      {staffMember.latestCurrentDayAttendance.departed && (
                        <>
                          <Label>{i18n.attendances.departureTime}</Label>{' '}
                          <span data-qa="departure-time">
                            {staffMember.latestCurrentDayAttendance.departed
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
              <ContentArea opaque paddingHorizontal="s">
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
                <FixedSpaceColumn alignItems="center">
                  {staffMember.present ? (
                    <LegacyButton
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
                    >
                      {i18n.attendances.staff.markDeparted}
                    </LegacyButton>
                  ) : (
                    <>
                      {!isOperationalDate && (
                        <H4 centered={true}>
                          {i18n.attendances.notOperationalDate}
                        </H4>
                      )}
                      <LegacyButton
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
                      >
                        {i18n.attendances.staff.markArrived}
                      </LegacyButton>
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
