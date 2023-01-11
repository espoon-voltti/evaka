// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H4, Label } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employeeMobile'

import { UnwrapResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import { UnitContext } from '../common/unit'

import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { TimeInfo } from './components/staff-components'
import { StaffAttendanceContext } from './state'
import { toStaff } from './utils'

export default React.memo(function StaffMemberPage() {
  const { unitId, groupId, employeeId } = useNonNullableParams<{
    unitId: string
    groupId: string
    employeeId: string
  }>()
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { staffAttendanceResponse } = useContext(StaffAttendanceContext)
  const { unitInfoResponse } = useContext(UnitContext)

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

  return (
    <StaffMemberPageContainer>
      <UnwrapResult result={employeeResponse}>
        {({ isOperationalDate, staffMember }) =>
          staffMember === undefined ? (
            <ErrorSegment
              title={i18n.attendances.staff.errors.employeeNotFound}
            />
          ) : (
            <>
              <EmployeeCardBackground staff={toStaff(staffMember)} />
              <FixedSpaceColumn>
                {featureFlags.experimental?.staffAttendanceTypes ? (
                  <>
                    {staffMember.spanningPlan && (
                      <TimeInfo data-qa="shift-time">
                        <span>
                          {i18n.attendances.staff.plannedAttendance}{' '}
                          {staffMember.spanningPlan.start
                            .toLocalTime()
                            .format()}
                          –{staffMember.spanningPlan.end.toLocalTime().format()}
                        </span>
                      </TimeInfo>
                    )}
                    {staffMember.attendances.length > 0 &&
                      orderBy(staffMember.attendances, ({ arrived }) =>
                        arrived.formatIso()
                      ).map(({ arrived, departed, type }) => (
                        <TimeInfo
                          key={arrived.formatIso()}
                          data-qa="attendance-time"
                        >
                          <Label>{i18n.attendances.staffTypes[type]}</Label>{' '}
                          <span>
                            {arrived.toLocalTime().format()}–
                            {departed?.toLocalTime().format() ?? ''}
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
                      {staffMember.latestCurrentDayAttendance.departed && (
                        <TimeInfo>
                          <Label>{i18n.attendances.departureTime}</Label>{' '}
                          <span data-qa="departure-time">
                            {staffMember.latestCurrentDayAttendance.departed
                              ?.toLocalTime()
                              .format()}
                          </span>
                        </TimeInfo>
                      )}
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
                            `/units/${unitId}/groups/${groupId}/staff-attendance/${staffMember.employeeId}/mark-departed`
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
                              `/units/${unitId}/groups/${groupId}/staff-attendance/${staffMember.employeeId}/mark-arrived`
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
          )
        }
      </UnwrapResult>
    </StaffMemberPageContainer>
  )
})
