// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatTime } from 'lib-common/date'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import React, { useContext } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { renderResult } from '../async-rendering'
import { WideLinkButton } from '../mobile/components'
import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { TimeInfo } from './components/staff-components'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { toStaff } from './staff'

export default React.memo(function StaffMemberPage() {
  const { unitId, groupId, employeeId } = useParams<{
    unitId: string
    groupId: string
    employeeId: string
  }>()
  const { i18n } = useTranslation()

  const { staffAttendanceResponse } = useContext(StaffAttendanceContext)

  const staffMember = staffAttendanceResponse.map((res) =>
    res.staff.find((s) => s.employeeId === employeeId)
  )

  return (
    <StaffMemberPageContainer>
      {renderResult(staffMember, (staffMember) =>
        staffMember === undefined ? (
          <ErrorSegment
            title={i18n.attendances.staff.errors.employeeNotFound}
          />
        ) : (
          <>
            <EmployeeCardBackground staff={toStaff(staffMember)} />
            <FixedSpaceColumn>
              {staffMember.latestCurrentDayAttendance && (
                <>
                  <TimeInfo>
                    <Label>{i18n.attendances.arrivalTime}</Label>{' '}
                    {formatTime(staffMember.latestCurrentDayAttendance.arrived)}
                  </TimeInfo>
                  {staffMember.latestCurrentDayAttendance.departed && (
                    <TimeInfo>
                      <Label>{i18n.attendances.departureTime}</Label>{' '}
                      {formatTime(
                        staffMember.latestCurrentDayAttendance.departed
                      )}
                    </TimeInfo>
                  )}
                </>
              )}
              {staffMember.present ? (
                <WideLinkButton
                  $primary
                  data-qa="mark-departed-link"
                  to={`/units/${unitId}/groups/${groupId}/staff-attendance/${staffMember.employeeId}/mark-departed`}
                >
                  {i18n.attendances.staff.markDeparted}
                </WideLinkButton>
              ) : (
                <WideLinkButton
                  $primary
                  data-qa="mark-arrived-link"
                  to={`/units/${unitId}/groups/${groupId}/staff-attendance/${staffMember.employeeId}/mark-arrived`}
                >
                  {i18n.attendances.staff.markArrived}
                </WideLinkButton>
              )}
            </FixedSpaceColumn>
          </>
        )
      )}
    </StaffMemberPageContainer>
  )
})
