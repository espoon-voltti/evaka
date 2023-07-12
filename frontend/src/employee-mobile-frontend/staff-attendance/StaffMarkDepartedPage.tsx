// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter } from 'date-fns'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { mockNow } from 'lib-common/utils/helpers'
import Title from 'lib-components/atoms/Title'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { EMPTY_PIN, PinInput } from 'lib-components/molecules/PinInput'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import TopBar from '../common/TopBar'
import { Actions, CustomTitle } from '../common/components'
import { TimeWrapper } from '../common/components'
import { useTranslation } from '../common/i18n'
import { UnitContext } from '../common/unit'
import { TallContentArea } from '../pairing/components'

import StaffAttendanceTypeSelection from './components/StaffAttendanceTypeSelection'
import { staffAttendanceQuery, staffDepartureMutation } from './queries'
import { getAttendanceDepartureDifferenceReasons } from './utils'

export default React.memo(function StaffMarkDepartedPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { unitId, groupId, employeeId } = useNonNullableParams<{
    unitId: string
    groupId: string
    employeeId: string
  }>()

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  useEffect(() => {
    reloadUnitInfo()
  }, [reloadUnitInfo])

  const staffAttendanceResponse = useQueryResult(staffAttendanceQuery(unitId))

  const staffMember = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        res.staff.find((s) => s.employeeId === employeeId)
      ),
    [employeeId, staffAttendanceResponse]
  )

  const [pinCode, setPinCode] = useState(EMPTY_PIN)
  const [time, setTime] = useState<string>(() =>
    HelsinkiDateTime.now().toLocalTime().format()
  )
  const [now, setNow] = useState<Date>(() => mockNow() ?? new Date())

  const [errorCode, setErrorCode] = useState<string | undefined>(undefined)
  const [attendanceType, setAttendanceType] = useState<StaffAttendanceType>()

  const isValidTimeString = (time: string) =>
    LocalTime.tryParse(time) !== undefined

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
        const groupId = staffMember?.latestCurrentDayAttendance?.groupId
        return { staffMember, attendanceId, groupId }
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

  const timeInFuture = useMemo(
    () =>
      isValidTime(time) &&
      isAfter(
        HelsinkiDateTime.now().withTime(LocalTime.parse(time)).toSystemTzDate(),
        now
      ),
    [time, now]
  )

  const staffAttendanceDifferenceReasons: StaffAttendanceType[] = useMemo(
    () =>
      staffMember
        .map((staff) => {
          const parsedTime = LocalTime.tryParse(time)
          if (!parsedTime || !staff?.spanningPlan) return []
          const departed = HelsinkiDateTime.fromLocal(
            LocalDate.todayInHelsinkiTz(),
            parsedTime
          )
          return getAttendanceDepartureDifferenceReasons(
            staff.spanningPlan.end,
            departed
          )
        })
        .getOrElse([]),
    [staffMember, time]
  )

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

            const confirmDisabled =
              pinLocked ||
              !pinSet ||
              !isValidTimeString(time) ||
              timeInFuture ||
              pinCode.join('').trim().length < 4

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
                    onFocus={() => setNow(mockNow() ?? new Date())}
                    value={time}
                    data-qa="set-time"
                    info={
                      timeInFuture
                        ? {
                            status: 'warning',
                            text: i18n.common.validation.dateLte(
                              formatTime(now)
                            )
                          }
                        : undefined
                    }
                  />
                  {timeInFuture && (
                    <InfoBoxWrapper>
                      <InfoBox
                        message={i18n.attendances.departureCannotBeDoneInFuture}
                        data-qa="departure-cannot-be-done-in-future-notification"
                      />
                    </InfoBoxWrapper>
                  )}
                  {!timeInFuture &&
                    staffAttendanceDifferenceReasons.length > 0 && (
                      <StaffAttendanceTypeSelection
                        i18n={i18n}
                        types={staffAttendanceDifferenceReasons}
                        selectedType={attendanceType}
                        setSelectedType={setAttendanceType}
                      />
                    )}
                </TimeWrapper>
                <Gap size="xs" />
                <Actions>
                  <FixedSpaceRow fullWidth>
                    <Button
                      text={i18n.common.cancel}
                      onClick={() => navigate(-1)}
                    />
                    <MutateButton
                      primary
                      text={i18n.common.confirm}
                      disabled={confirmDisabled}
                      mutation={staffDepartureMutation}
                      onClick={() => {
                        const groupId = memberAttendance
                          .map(({ groupId }) => groupId)
                          .getOrElse(undefined)

                        if (groupId) {
                          return {
                            unitId,
                            request: {
                              employeeId,
                              groupId,
                              time: LocalTime.parse(time),
                              pinCode: pinCode.join(''),
                              type: attendanceType ?? null
                            }
                          }
                        } else {
                          return cancelMutation
                        }
                      }}
                      onSuccess={() => {
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

const InfoBoxWrapper = styled.div`
  font-size: 16px;
`
