// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter, parse } from 'date-fns'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { mockNow } from 'lib-common/utils/helpers'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { EMPTY_PIN, PinInput } from 'lib-components/molecules/PinInput'
import { Gap } from 'lib-components/white-space'

import { postStaffDeparture } from '../../api/realtimeStaffAttendances'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import { Actions, CustomTitle } from '../attendances/components'
import { TimeWrapper } from '../attendances/components'
import TopBar from '../common/TopBar'
import { TallContentArea } from '../mobile/components'

export default React.memo(function StaffMarkDepartedPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { unitId, groupId, employeeId } = useNonNullableParams<{
    unitId: string
    groupId: string
    employeeId: string
  }>()

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  useEffect(reloadUnitInfo, [reloadUnitInfo])

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )

  const [pinCode, setPinCode] = useState(EMPTY_PIN)
  const [time, setTime] = useState<string>(() =>
    HelsinkiDateTime.now().toLocalTime().format('HH:mm')
  )
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined)

  const staffInfo = useMemo(
    () =>
      unitInfoResponse.map((res) => {
        const staffInfo = res.staff.find((s) => s.id === employeeId)
        const pinSet = staffInfo?.pinSet ?? true
        const pinLocked = staffInfo?.pinLocked || errorCode === 'PIN_LOCKED'
        return { pinSet, pinLocked }
      }),
    [employeeId, errorCode, unitInfoResponse]
  )

  const memberAttendance = useMemo(
    () =>
      staffAttendanceResponse.map((res) => {
        const staffMember = res.staff.find((s) => s.employeeId === employeeId)
        const attendanceId = staffMember?.attendances.find(
          ({ departed }) => departed === null
        )?.id
        return { staffMember, attendanceId }
      }),
    [employeeId, staffAttendanceResponse]
  )

  const backButtonText = useMemo(
    () =>
      memberAttendance
        .map(({ staffMember }) =>
          staffMember
            ? `${staffMember.firstName} ${staffMember.lastName}`
            : i18n.common.back
        )
        .getOrElse(i18n.common.back),
    [memberAttendance, i18n.common.back]
  )

  const now = mockNow() ?? new Date()
  const timeInFuture = isAfter(parse(time, 'HH:mm', now), now)

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <TopBar
        title={backButtonText}
        onBack={() => navigate(-1)}
        invertedColors
      />
      <ContentArea
        shadow
        opaque={true}
        paddingHorizontal="s"
        paddingVertical="m"
      >
        {renderResult(
          combine(staffInfo, memberAttendance),
          ([{ pinSet, pinLocked }, { staffMember, attendanceId }]) => {
            if (staffMember === undefined) {
              return (
                <Navigate
                  replace
                  to={`/units/${unitId}/groups/${groupId}/staff-attendance`}
                />
              )
            }
            if (attendanceId === undefined) {
              return (
                <Navigate
                  replace
                  to={`/units/${unitId}/groups/${groupId}/staff-attendance/${employeeId}`}
                />
              )
            }

            return (
              <>
                <Title centered noMargin>
                  {i18n.attendances.staff.loginWithPin}
                </Title>
                <Gap />
                {!pinSet ? (
                  <ErrorSegment title={i18n.attendances.staff.pinNotSet} />
                ) : pinLocked ? (
                  <ErrorSegment title={i18n.attendances.staff.pinLocked} />
                ) : (
                  <PinInput
                    pin={pinCode}
                    onPinChange={setPinCode}
                    invalid={errorCode === 'WRONG_PIN'}
                  />
                )}
                <Gap />
                <TimeWrapper>
                  <CustomTitle>{i18n.attendances.departureTime}</CustomTitle>
                  <TimeInput
                    onChange={setTime}
                    value={time}
                    data-qa="set-time"
                    info={
                      timeInFuture
                        ? {
                            status: 'warning',
                            text: i18n.common.validation.dateLte(
                              formatTime(mockNow() ?? new Date())
                            )
                          }
                        : undefined
                    }
                  />
                  <Gap />
                </TimeWrapper>
                <Gap size="xs" />
                <Actions>
                  <FixedSpaceRow fullWidth>
                    <Button
                      text={i18n.common.cancel}
                      onClick={() => navigate(-1)}
                    />
                    <AsyncButton
                      primary
                      text={i18n.common.confirm}
                      disabled={
                        pinLocked ||
                        !pinSet ||
                        !isValidTime(time) ||
                        timeInFuture ||
                        pinCode.join('').trim().length < 4
                      }
                      onClick={() =>
                        postStaffDeparture(attendanceId, {
                          time: LocalTime.parse(time, 'HH:mm'),
                          pinCode: pinCode.join('')
                        })
                      }
                      onSuccess={() => {
                        reloadStaffAttendance()
                        history.go(-1)
                      }}
                      onFailure={(res) => {
                        setErrorCode(res.errorCode)
                        if (res.errorCode === 'WRONG_PIN') {
                          setPinCode(EMPTY_PIN)
                        }
                      }}
                      data-qa="mark-departed-btn"
                    />
                  </FixedSpaceRow>
                </Actions>
              </>
            )
          }
        )}
      </ContentArea>
    </TallContentArea>
  )
})
