// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { formatTime } from 'lib-common/date'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { mockNow } from 'lib-common/utils/helpers'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'

import { postExternalStaffDeparture } from '../../api/realtimeStaffAttendances'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnwrapResult } from '../async-rendering'

import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { TimeInfo } from './components/staff-components'
import { toStaff } from './staff'

export default React.memo(function ExternalStaffMemberPage() {
  const navigate = useNavigate()
  const { attendanceId } = useNonNullableParams<{
    attendanceId: string
  }>()
  const { i18n } = useTranslation()

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )

  const attendance = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        res.extraAttendances.find((s) => s.id === attendanceId)
      ),
    [attendanceId, staffAttendanceResponse]
  )

  const [time, setTime] = useState(formatTime(mockNow() ?? new Date()))

  return (
    <StaffMemberPageContainer>
      <UnwrapResult result={attendance}>
        {(ext) =>
          !ext ? (
            <ErrorSegment
              title={i18n.attendances.staff.errors.employeeNotFound}
            />
          ) : (
            <>
              <EmployeeCardBackground staff={toStaff(ext)} />
              <FixedSpaceColumn>
                <TimeInfo>
                  <Label>{i18n.attendances.arrivalTime}</Label>{' '}
                  <span data-qa="arrival-time">{formatTime(ext.arrived)}</span>
                </TimeInfo>

                <TimeInfo>
                  <Label htmlFor="time-input">
                    {i18n.attendances.departureTime}
                  </Label>
                  <TimeInput
                    id="time-input"
                    data-qa="departure-time-input"
                    value={time}
                    onChange={(val) => setTime(val)}
                  />
                </TimeInfo>

                <AsyncButton
                  primary
                  text={i18n.attendances.actions.markDeparted}
                  data-qa="mark-departed-link"
                  onClick={() =>
                    postExternalStaffDeparture({ attendanceId, time })
                  }
                  onSuccess={() => {
                    reloadStaffAttendance()
                    navigate(-1)
                  }}
                />
              </FixedSpaceColumn>
            </>
          )
        }
      </UnwrapResult>
    </StaffMemberPageContainer>
  )
})
