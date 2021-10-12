// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatTime } from 'lib-common/date'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import InputField from 'lib-components/atoms/form/InputField'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { postExternalStaffDeparture } from '../../api/staffAttendances'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { renderResult } from '../async-rendering'
import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { TimeInfo } from './components/staff-components'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { toStaff } from './staff'

export default React.memo(function ExternalStaffMemberPage() {
  const history = useHistory()
  const { attendanceId } = useParams<{
    attendanceId: string
  }>()
  const { i18n } = useTranslation()

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )
  useEffect(reloadStaffAttendance, [reloadStaffAttendance])

  const attendance = staffAttendanceResponse.map((res) =>
    res.extraAttendances.find((s) => s.id === attendanceId)
  )

  const [time, setTime] = useState(formatTime(new Date()))

  return (
    <StaffMemberPageContainer>
      {renderResult(attendance, (ext) =>
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
                {formatTime(ext.arrived)}
              </TimeInfo>

              <TimeInfo>
                <Label htmlFor="time-input">
                  {i18n.attendances.departureTime}
                </Label>
                <InputField
                  id="time-input"
                  type="time"
                  width="s"
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
                onSuccess={() => history.goBack()}
              />
            </FixedSpaceColumn>
          </>
        )
      )}
    </StaffMemberPageContainer>
  )
})
