// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faEdit } from '@fortawesome/free-solid-svg-icons'
import orderBy from 'lodash/orderBy'
import React, { useContext, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'

import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { TimeInfo } from './components/staff-components'
import { staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

export default React.memo(function StaffMemberPage() {
  const { unitId, employeeId } = useRequiredParams('unitId', 'employeeId')
  const { groupRoute } = useSelectedGroup()
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { unitInfoResponse } = useContext(UnitContext)

  const staffAttendanceResponse = useQueryResult(staffAttendanceQuery(unitId))

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

  return renderResult(
    employeeResponse,
    ({ isOperationalDate, staffMember }) => (
      <StaffMemberPageContainer back={`${groupRoute}/staff-attendance/present`}>
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
                  {staffMember.spanningPlan && (
                    <TimeInfo data-qa="shift-time">
                      <span>
                        {i18n.attendances.staff.plannedAttendance}{' '}
                        {staffMember.spanningPlan.start.toLocalTime().format()}–
                        {staffMember.spanningPlan.end.toLocalTime().format()}
                      </span>
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
                                  `${groupRoute}/staff-attendance/${staffMember.employeeId}/edit`
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
                            `${groupRoute}/staff-attendance/${staffMember.employeeId}/edit`
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
                <FixedSpaceColumn>
                  {staffMember.present ? (
                    <Button
                      primary
                      data-qa="mark-departed-btn"
                      onClick={() =>
                        navigate(
                          `${groupRoute}/staff-attendance/${staffMember.employeeId}/mark-departed`
                        )
                      }
                    >
                      {i18n.attendances.staff.markDeparted}
                    </Button>
                  ) : (
                    <>
                      {!isOperationalDate && (
                        <H4 centered={true}>
                          {i18n.attendances.notOperationalDate}
                        </H4>
                      )}
                      <Button
                        primary
                        data-qa="mark-arrived-btn"
                        disabled={!isOperationalDate}
                        onClick={() =>
                          navigate(
                            `${groupRoute}/staff-attendance/${staffMember.employeeId}/mark-arrived`
                          )
                        }
                      >
                        {i18n.attendances.staff.markArrived}
                      </Button>
                    </>
                  )}
                </FixedSpaceColumn>
              </ContentArea>
            </FixedSpaceColumn>
          </>
        )}
      </StaffMemberPageContainer>
    )
  )
})

const InlineIconButton = styled(IconButton)`
  display: inline-flex;
  margin-left: ${defaultMargins.xxs};
`
