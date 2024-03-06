// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addMinutes, differenceInMinutes, subMinutes } from 'date-fns'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { formatTime } from 'lib-common/date'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRequiredParams from 'lib-common/useRequiredParams'
import { mockNow } from 'lib-common/utils/helpers'
import Title from 'lib-components/atoms/Title'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
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
import { staffArrivalMutation, staffAttendanceQuery } from './queries'
import { getAttendanceArrivalDifferenceReasons } from './utils'

export default React.memo(function StaffMarkArrivedPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { unitId, employeeId } = useRequiredParams('unitId', 'employeeId')
  const { selectedGroupId } = useSelectedGroup()

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
  const pinInputRef = useRef<HTMLInputElement>(null)
  const [timeStr, setTimeStr] = useState<string>(() =>
    HelsinkiDateTime.now().toLocalTime().format()
  )
  const time = useMemo(() => LocalTime.tryParse(timeStr), [timeStr])

  const [forceUpdateState, updateState] = useState<boolean>(true)
  const forceRerender = React.useCallback(
    () => updateState(!forceUpdateState),
    [forceUpdateState]
  )

  const [attendanceGroup, setAttendanceGroup] = useState<UUID | undefined>(
    selectedGroupId.type !== 'all' ? selectedGroupId.id : undefined
  )
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined)
  const [attendanceType, setAttendanceType] = useState<StaffAttendanceType>()

  const getNow = () => mockNow() ?? new Date()

  const groupOptions = useMemo(
    () =>
      unitInfoResponse.map((unitInfoResponse) =>
        unitInfoResponse.groups.map(({ id }) => id)
      ),
    [unitInfoResponse]
  )

  useEffect(() => {
    if (
      attendanceGroup === undefined &&
      groupOptions.isSuccess &&
      groupOptions.value.length === 1
    ) {
      setAttendanceGroup(groupOptions.value[0])
    }
  }, [attendanceGroup, groupOptions])

  const firstPlannedStartOfTheDay = useMemo(
    () =>
      staffMember
        .map((staff) =>
          staff && staff.plannedAttendances.length > 0
            ? staff.plannedAttendances.reduce(
                (prev, curr) => (prev.start.isBefore(curr.start) ? prev : curr),
                staff.plannedAttendances[0]
              ).start
            : null
        )
        .getOrElse(null),
    [staffMember]
  )

  const selectedTimeDiffFromPlannedStartOfDayMinutes = useMemo(
    () =>
      firstPlannedStartOfTheDay && time !== undefined
        ? differenceInMinutes(
            HelsinkiDateTime.now().withTime(time).toSystemTzDate(),
            firstPlannedStartOfTheDay.toSystemTzDate()
          )
        : null,
    [firstPlannedStartOfTheDay, time]
  )

  const selectedTimeIsWithin30MinsFromNow = useCallback(
    (now: Date) =>
      time !== undefined &&
      Math.abs(
        differenceInMinutes(
          HelsinkiDateTime.now().withTime(time).toSystemTzDate(),
          now
        )
      ) <= 30,
    [time]
  )

  const hasPlan = useMemo(
    () => firstPlannedStartOfTheDay != null,
    [firstPlannedStartOfTheDay]
  )

  const backButtonText = useMemo(
    () =>
      staffMember
        .map((staffMember) =>
          staffMember
            ? `${staffMember.firstName} ${staffMember.lastName}`
            : i18n.common.back
        )
        .getOrElse(i18n.common.back),
    [i18n.common.back, staffMember]
  )

  const staffAttendanceDifferenceReasons: StaffAttendanceType[] = useMemo(
    () =>
      staffMember
        .map((staff) => {
          if (time === undefined || !staff?.spanningPlan) return []
          const arrived = HelsinkiDateTime.fromLocal(
            LocalDate.todayInHelsinkiTz(),
            time
          )
          return getAttendanceArrivalDifferenceReasons(
            staff.spanningPlan.start,
            arrived
          )
        })
        .getOrElse([]),
    [staffMember, time]
  )

  const showAttendanceTypeSelection = useMemo(() => {
    if (!hasPlan || !selectedTimeDiffFromPlannedStartOfDayMinutes) return false
    if (Math.abs(selectedTimeDiffFromPlannedStartOfDayMinutes) <= 5)
      return false
    return staffAttendanceDifferenceReasons.length >= 1
  }, [
    selectedTimeDiffFromPlannedStartOfDayMinutes,
    staffAttendanceDifferenceReasons,
    hasPlan
  ])

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
          combine(unitInfoResponse, staffMember),
          ([unitInfo, staffMember]) => {
            if (staffMember === undefined)
              return (
                <ErrorSegment
                  title={i18n.attendances.staff.errors.employeeNotFound}
                />
              )

            const staffInfo = unitInfo.staff.find((s) => s.id === employeeId)
            const pinSet = staffInfo?.pinSet ?? true
            const pinLocked = staffInfo?.pinLocked || errorCode === 'PIN_LOCKED'

            const latestCurrentDayDepartureTime =
              staffMember.latestCurrentDayAttendance?.departed?.toLocalTime()
            const timeBeforeLastDeparture =
              latestCurrentDayDepartureTime !== undefined &&
              time !== undefined &&
              latestCurrentDayDepartureTime.isAfter(time)
                ? latestCurrentDayDepartureTime
                : undefined

            const disableConfirmBecauseOfPlan =
              showAttendanceTypeSelection &&
              selectedTimeDiffFromPlannedStartOfDayMinutes != null &&
              selectedTimeDiffFromPlannedStartOfDayMinutes < -5 &&
              attendanceType == null

            const confirmDisabled =
              pinLocked ||
              !pinSet ||
              time === undefined ||
              pinCode.join('').trim().length < 4 ||
              !selectedTimeIsWithin30MinsFromNow(getNow()) ||
              !attendanceGroup ||
              disableConfirmBecauseOfPlan ||
              timeBeforeLastDeparture !== undefined

            const hasFutureCurrentDay =
              staffMember.latestCurrentDayAttendance &&
              (!staffMember.latestCurrentDayAttendance.departed ||
                (time !== undefined &&
                  staffMember.latestCurrentDayAttendance.arrived.isAfter(
                    HelsinkiDateTime.now().withTime(time)
                  )))

            const hasConflictingFuture =
              staffMember.hasFutureAttendances || !!hasFutureCurrentDay

            return (
              <>
                <Title centered noMargin>
                  {i18n.attendances.staff.loginWithPin}
                </Title>
                {hasConflictingFuture && (
                  <AlertBox
                    message={i18n.attendances.staff.hasFutureAttendance}
                    wide
                  />
                )}
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
                  <CustomTitle>{i18n.attendances.arrivalTime}</CustomTitle>
                  <TimeInput
                    onChange={setTimeStr}
                    value={timeStr}
                    data-qa="input-arrived"
                    info={
                      !selectedTimeIsWithin30MinsFromNow(getNow())
                        ? {
                            status: 'warning',
                            text: i18n.common.validation.dateBetween(
                              formatTime(subMinutes(getNow(), 30)),
                              formatTime(addMinutes(getNow(), 30))
                            )
                          }
                        : undefined
                    }
                  />
                  {!selectedTimeIsWithin30MinsFromNow(getNow()) && (
                    <InfoBoxWrapper>
                      <InfoBox
                        message={i18n.attendances.timeDiffTooBigNotification}
                        data-qa="time-diff-too-big-notification"
                      />
                    </InfoBoxWrapper>
                  )}
                  {timeBeforeLastDeparture !== undefined && (
                    <InfoBoxWrapper>
                      <InfoBox
                        message={i18n.attendances.arrivalIsBeforeDeparture(
                          timeBeforeLastDeparture.format()
                        )}
                        data-qa="arrival-before-departure-notification"
                      />
                    </InfoBoxWrapper>
                  )}
                  {showAttendanceTypeSelection && (
                    <StaffAttendanceTypeSelection
                      i18n={i18n}
                      types={staffAttendanceDifferenceReasons}
                      selectedType={attendanceType}
                      setSelectedType={setAttendanceType}
                    />
                  )}
                  {selectedTimeIsWithin30MinsFromNow(getNow()) &&
                    renderResult(groupOptions, (groupOptions) =>
                      groupOptions.length > 1 ? (
                        <>
                          <Gap />
                          <CustomTitle>{i18n.common.group}</CustomTitle>
                          <Select
                            data-qa="group-select"
                            selectedItem={attendanceGroup}
                            items={groupOptions}
                            getItemLabel={(item) =>
                              unitInfo.groups.find((group) => group.id === item)
                                ?.name ?? ''
                            }
                            placeholder={i18n.attendances.chooseGroup}
                            onChange={(group) =>
                              setAttendanceGroup(group ?? undefined)
                            }
                          />
                        </>
                      ) : null
                    )}
                  <Gap />
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
                      mutation={staffArrivalMutation}
                      onClick={() => {
                        if (selectedTimeIsWithin30MinsFromNow(getNow()))
                          if (time !== undefined && attendanceGroup) {
                            return {
                              unitId,
                              request: {
                                employeeId,
                                groupId: attendanceGroup,
                                time,
                                pinCode: pinCode.join(''),
                                type: attendanceType ?? null
                              }
                            }
                          } else {
                            return cancelMutation
                          }
                        else {
                          forceRerender()
                          return cancelMutation
                        }
                      }}
                      onSuccess={() => {
                        history.go(-1)
                      }}
                      onFailure={({ errorCode }) => {
                        setErrorCode(errorCode)
                        if (errorCode === 'WRONG_PIN') {
                          setPinCode(EMPTY_PIN)
                          pinInputRef.current?.focus()
                        }
                      }}
                      data-qa="mark-arrived-btn"
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
