// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter, parse } from 'date-fns'
import { combine, Success } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InputField from 'lib-components/atoms/form/InputField'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'
import React, { useContext, useEffect, useRef, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { EMPTY_PIN, PinInput } from 'lib-components/molecules/PinInput'
import { postStaffArrival } from '../../api/staffAttendances'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import { Actions, BackButtonInline } from '../attendances/components'
import { TallContentArea } from '../mobile/components'
import { TimeWrapper } from './components/staff-components'
import { Label } from 'lib-components/typography'

export default React.memo(function StaffMarkArrivedPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { groupId, employeeId } = useParams<{
    groupId: UUID | 'all'
    employeeId: UUID
  }>()

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  useEffect(() => reloadUnitInfo(true), [reloadUnitInfo])

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )
  useEffect(() => reloadStaffAttendance(true), [reloadStaffAttendance])

  const staffMember = staffAttendanceResponse.map((res) =>
    res.staff.find((s) => s.employeeId === employeeId)
  )

  const [pinCode, setPinCode] = useState(EMPTY_PIN)
  const pinInputRef = useRef<HTMLInputElement>(null)
  const [time, setTime] = useState<string>(formatTime(new Date()))
  const [attendanceGroup, setAttendanceGroup] = useState<UUID | undefined>(
    groupId !== 'all' ? groupId : undefined
  )
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined)

  const groupOptions =
    groupId === 'all'
      ? combine(staffMember, unitInfoResponse).map(
          ([staffMember, unitInfoResponse]) => {
            const groupIds = staffMember?.groupIds ?? []
            return unitInfoResponse.groups
              .filter((g) => groupIds.length === 0 || groupIds.includes(g.id))
              .map((g) => ({ value: g.id, label: g.name }))
          }
        )
      : Success.of([])

  useEffect(() => {
    if (
      attendanceGroup === undefined &&
      groupOptions.isSuccess &&
      groupOptions.value.length === 1
    ) {
      setAttendanceGroup(groupOptions.value[0].value)
    }
  }, [attendanceGroup, groupOptions])

  const timeInFuture = isAfter(parse(time, 'HH:mm', new Date()), new Date())

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <div>
        <BackButtonInline
          onClick={() => history.goBack()}
          icon={faArrowLeft}
          text={
            staffMember.isSuccess && staffMember.value
              ? `${staffMember.value.firstName} ${staffMember.value.lastName}`
              : i18n.common.back
          }
        />
      </div>
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
                  <Label>{i18n.attendances.arrivalTime}</Label>
                  <InputField
                    onChange={setTime}
                    value={time}
                    width="s"
                    type="time"
                    data-qa="set-time"
                    info={
                      timeInFuture
                        ? {
                            status: 'warning',
                            text: i18n.common.validation.dateLte(
                              formatTime(new Date())
                            )
                          }
                        : undefined
                    }
                  />
                  {renderResult(groupOptions, (groupOptions) =>
                    groupOptions.length > 1 ? (
                      <>
                        <Gap />
                        <Label>{i18n.common.group}</Label>
                        <SimpleSelect
                          value={attendanceGroup}
                          placeholder={i18n.attendances.chooseGroup}
                          options={groupOptions}
                          onChange={(e) => setAttendanceGroup(e.target.value)}
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
                      onClick={() => history.goBack()}
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
                              time,
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
                          : Promise.reject()
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
