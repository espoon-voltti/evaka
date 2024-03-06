// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
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
import { Actions, CustomTitle, TimeWrapper } from '../common/components'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'
import { TallContentArea } from '../pairing/components'

import StaffAttendanceTypeSelection from './components/StaffAttendanceTypeSelection'
import { staffAttendanceQuery, staffDepartureMutation } from './queries'
import { getAttendanceDepartureDifferenceReasons } from './utils'

export default React.memo(function StaffMarkDepartedPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { unitId, employeeId } = useRequiredParams('unitId', 'employeeId')

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  const { groupRoute } = useSelectedGroup()
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
  const [timeStr, setTimeStr] = useState<string>(() =>
    HelsinkiDateTime.now().toLocalTime().format()
  )
  const time = useMemo(() => LocalTime.tryParse(timeStr), [timeStr])

  const [now, setNow] = useState(() => HelsinkiDateTime.now())

  const [errorCode, setErrorCode] = useState<string | undefined>(undefined)
  const [attendanceType, setAttendanceType] = useState<StaffAttendanceType>()

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
        const latestCurrentDayAttendance =
          staffMember?.latestCurrentDayAttendance
        const latestCurrentDayArrival =
          latestCurrentDayAttendance &&
          latestCurrentDayAttendance.arrived
            .toLocalDate()
            .isEqual(LocalDate.todayInHelsinkiTz())
            ? latestCurrentDayAttendance.arrived.toLocalTime()
            : undefined
        const groupId = staffMember?.latestCurrentDayAttendance?.groupId
        return {
          staffMember,
          attendanceId,
          groupId,
          latestCurrentDayArrival
        }
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
      time !== undefined && HelsinkiDateTime.now().withTime(time).isAfter(now),
    [time, now]
  )

  const staffAttendanceDifferenceReasons: StaffAttendanceType[] = useMemo(
    () =>
      staffMember
        .map((staff) => {
          if (time === undefined || !staff?.spanningPlan) return []
          const departed = HelsinkiDateTime.fromLocal(
            LocalDate.todayInHelsinkiTz(),
            time
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
          ([
            { pinSet, pinLocked },
            { staffMember, attendanceId, latestCurrentDayArrival }
          ]) => {
            if (staffMember === undefined) {
              return <Navigate replace to={`${groupRoute}/staff-attendance`} />
            }
            if (attendanceId === undefined) {
              return (
                <Navigate
                  replace
                  to={`${groupRoute}/staff-attendance/${employeeId}`}
                />
              )
            }

            const timeBeforeLastArrival =
              latestCurrentDayArrival !== undefined &&
              time !== undefined &&
              latestCurrentDayArrival.isEqualOrAfter(time)
                ? latestCurrentDayArrival
                : undefined

            const confirmDisabled =
              pinLocked ||
              !pinSet ||
              time === undefined ||
              timeInFuture ||
              timeBeforeLastArrival !== undefined ||
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
                    onChange={setTimeStr}
                    onFocus={() => setNow(HelsinkiDateTime.now())}
                    value={timeStr}
                    data-qa="set-time"
                    info={
                      timeInFuture
                        ? {
                            status: 'warning',
                            text: i18n.common.validation.dateLte(
                              now.toLocalTime().format()
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
                  {timeBeforeLastArrival && (
                    <InfoBoxWrapper>
                      <InfoBox
                        message={i18n.attendances.departureIsBeforeArrival(
                          timeBeforeLastArrival.format()
                        )}
                        data-qa="departure-before-arrival-notification"
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

                        if (groupId && time !== undefined) {
                          return {
                            unitId,
                            request: {
                              employeeId,
                              groupId,
                              time,
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
