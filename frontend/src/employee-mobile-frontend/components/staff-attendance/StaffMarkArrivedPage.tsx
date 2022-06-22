// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter, parse } from 'date-fns'
import React, { useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine, Success } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { mockNow } from 'lib-common/utils/helpers'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Select from 'lib-components/atoms/dropdowns/Select'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { EMPTY_PIN, PinInput } from 'lib-components/molecules/PinInput'
import { Gap } from 'lib-components/white-space'

import { postStaffArrival } from '../../api/realtimeStaffAttendances'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import { Actions, CustomTitle } from '../attendances/components'
import { TimeWrapper } from '../attendances/components'
import TopBar from '../common/TopBar'
import { TallContentArea } from '../mobile/components'

export default React.memo(function StaffMarkArrivedPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { groupId, employeeId } = useNonNullableParams<{
    groupId: UUID | 'all'
    employeeId: UUID
  }>()

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  useEffect(reloadUnitInfo, [reloadUnitInfo])

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )

  const staffMember = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        res.staff.find((s) => s.employeeId === employeeId)
      ),
    [employeeId, staffAttendanceResponse]
  )

  const [pinCode, setPinCode] = useState(EMPTY_PIN)
  const pinInputRef = useRef<HTMLInputElement>(null)
  const [time, setTime] = useState<string>(() =>
    HelsinkiDateTime.now().toLocalTime().format('HH:mm')
  )
  const [attendanceGroup, setAttendanceGroup] = useState<UUID | undefined>(
    groupId !== 'all' ? groupId : undefined
  )
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined)

  const groupOptions = useMemo(
    () =>
      groupId === 'all'
        ? combine(staffMember, unitInfoResponse).map(
            ([staffMember, unitInfoResponse]) => {
              const groupIds = staffMember?.groupIds ?? []
              return unitInfoResponse.groups
                .filter((g) => groupIds.length === 0 || groupIds.includes(g.id))
                .map(({ id }) => id)
            }
          )
        : Success.of([]),
    [groupId, staffMember, unitInfoResponse]
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

  const now = mockNow() ?? new Date()
  const timeInFuture = isAfter(parse(time, 'HH:mm', now), now)

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
                  <CustomTitle>{i18n.attendances.arrivalTime}</CustomTitle>
                  <TimeInput
                    onChange={setTime}
                    value={time}
                    data-qa="input-arrived"
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
                  {renderResult(groupOptions, (groupOptions) =>
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
                    <AsyncButton
                      primary
                      text={i18n.common.confirm}
                      disabled={
                        pinLocked ||
                        !pinSet ||
                        !isValidTime(time) ||
                        timeInFuture ||
                        pinCode.join('').trim().length < 4 ||
                        !attendanceGroup
                      }
                      onClick={() =>
                        attendanceGroup
                          ? postStaffArrival({
                              employeeId,
                              groupId: attendanceGroup,
                              time: LocalTime.parse(time, 'HH:mm'),
                              pinCode: pinCode.join('')
                            }).then((res) => {
                              if (res.isFailure) {
                                setErrorCode(res.errorCode)
                                if (res.errorCode === 'WRONG_PIN') {
                                  setPinCode(EMPTY_PIN)
                                  pinInputRef.current?.focus()
                                }
                              }
                              return res
                            })
                          : undefined
                      }
                      onSuccess={() => {
                        reloadStaffAttendance()
                        history.go(-1)
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
