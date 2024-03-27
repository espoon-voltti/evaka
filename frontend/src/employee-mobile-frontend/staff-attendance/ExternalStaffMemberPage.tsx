// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'

import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { TimeInfo } from './components/staff-components'
import { externalStaffDepartureMutation, staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

export default React.memo(function ExternalStaffMemberPage({
  unitId
}: {
  unitId: UUID
}) {
  const navigate = useNavigate()
  const { attendanceId } = useRouteParams(['attendanceId'])
  const { i18n } = useTranslation()

  const staffAttendanceResponse = useQueryResult(staffAttendanceQuery(unitId))

  const attendance = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        res.extraAttendances.find((s) => s.id === attendanceId)
      ),
    [attendanceId, staffAttendanceResponse]
  )

  const [time, setTime] = useState(() =>
    HelsinkiDateTime.now().toLocalTime().format()
  )
  const parsedTime = useMemo(() => LocalTime.tryParse(time), [time])

  return renderResult(attendance, (ext) => {
    if (!ext) {
      return (
        <StaffMemberPageContainer>
          <ErrorSegment
            title={i18n.attendances.staff.errors.employeeNotFound}
          />
        </StaffMemberPageContainer>
      )
    }

    const lastArrivalTime = ext.arrived
      .toLocalDate()
      .isEqual(HelsinkiDateTime.now().toLocalDate())
      ? ext.arrived.toLocalTime()
      : undefined
    const timeBeforeArrival =
      parsedTime !== undefined &&
      lastArrivalTime !== undefined &&
      parsedTime.isEqualOrBefore(lastArrivalTime)
        ? lastArrivalTime
        : undefined

    const departDisabled =
      parsedTime === undefined || timeBeforeArrival !== undefined

    return (
      <StaffMemberPageContainer>
        <EmployeeCardBackground staff={toStaff(ext)} />
        <FixedSpaceColumn>
          <TimeInfo>
            <Label>{i18n.attendances.arrivalTime}</Label>{' '}
            <span data-qa="arrival-time">
              {ext.arrived.toLocalTime().format()}
            </span>
          </TimeInfo>

          <TimeInfo>
            <Label htmlFor="time-input">{i18n.attendances.departureTime}</Label>
            <TimeInput
              id="time-input"
              data-qa="departure-time-input"
              value={time}
              onChange={(val) => setTime(val)}
            />
          </TimeInfo>

          {timeBeforeArrival !== undefined ? (
            <>
              <InfoBox
                thin
                message={i18n.attendances.departureIsBeforeArrival(
                  timeBeforeArrival.format()
                )}
                data-qa="departure-before-arrival-notification"
              />
              <Gap size="xs" />
            </>
          ) : undefined}

          <MutateButton
            primary
            text={i18n.attendances.actions.markDeparted}
            data-qa="mark-departed-btn"
            disabled={departDisabled}
            mutation={externalStaffDepartureMutation}
            onClick={() =>
              parsedTime !== undefined
                ? { unitId, request: { attendanceId, time: parsedTime } }
                : cancelMutation
            }
            onSuccess={() => {
              navigate(-1)
            }}
          />
        </FixedSpaceColumn>
      </StaffMemberPageContainer>
    )
  })
})
