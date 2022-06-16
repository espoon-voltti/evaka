// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import useNonNullableParams from 'lib-common/useNonNullableParams'
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

  const [time, setTime] = useState(() =>
    HelsinkiDateTime.now().toLocalTime().format('HH:mm')
  )
  const parsedTime = useMemo(() => LocalTime.tryParse(time, 'HH:mm'), [time])

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
                  <span data-qa="arrival-time">
                    {ext.arrived.toLocalTime().format('HH:mm')}
                  </span>
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
                  disabled={!parsedTime}
                  onClick={() =>
                    parsedTime &&
                    postExternalStaffDeparture({
                      attendanceId,
                      time: parsedTime
                    })
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
