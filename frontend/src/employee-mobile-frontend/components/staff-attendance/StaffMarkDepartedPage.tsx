// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import { isAfter, parse } from 'date-fns'
import { combine } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InputField from 'lib-components/atoms/form/InputField'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'
import { EMPTY_PIN, PinInput } from 'lib-components/molecules/PinInput'
import { postStaffDeparture } from '../../api/realtimeStaffAttendances'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import { Actions, BackButtonInline } from '../attendances/components'
import { TallContentArea } from '../mobile/components'
import { TimeWrapper } from './components/staff-components'
import { Label } from 'lib-components/typography'

export default React.memo(function StaffMarkDepartedPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitId, groupId, employeeId } = useParams<{
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
  const [time, setTime] = useState<string>(formatTime(new Date()))
  const [errorCode, setErrorCode] = useState<string | undefined>(undefined)

  const staffMember = staffAttendanceResponse.map((res) =>
    res.staff.find((s) => s.employeeId === employeeId)
  )
  if (staffMember.isSuccess && staffMember.value === undefined)
    return (
      <Redirect to={`/units/${unitId}/groups/${groupId}/staff-attendance`} />
    )

  const activeAttendanceId = staffMember.map((s) =>
    s?.latestCurrentDayAttendance &&
    s.latestCurrentDayAttendance.departed === null
      ? s.latestCurrentDayAttendance.id
      : undefined
  )
  if (activeAttendanceId.isSuccess && activeAttendanceId.value === undefined)
    return (
      <Redirect
        to={`/units/${unitId}/groups/${groupId}/staff-attendance/${employeeId}`}
      />
    )

  const timeInFuture = isAfter(parse(time, 'HH:mm', new Date()), new Date())

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal={'zero'}
      paddingVertical={'zero'}
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
        paddingHorizontal={'s'}
        paddingVertical={'m'}
      >
        {renderResult(
          combine(unitInfoResponse, staffMember, activeAttendanceId),
          ([unitInfo, staffMember, attendanceId]) => {
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
                  <Label>{i18n.attendances.departureTime}</Label>
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
                  <Gap />
                </TimeWrapper>
                <Gap size="xs" />
                <Actions>
                  <FixedSpaceRow fullWidth>
                    <Button
                      text={i18n.common.cancel}
                      onClick={() => history.goBack()}
                    />
                    {attendanceId && (
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
                            time,
                            pinCode: pinCode.join('')
                          }).then((res) => {
                            if (res.isFailure) {
                              setErrorCode(res.errorCode)
                              if (res.errorCode === 'WRONG_PIN') {
                                setPinCode(EMPTY_PIN)
                              }
                            }
                            return res
                          })
                        }
                        onSuccess={() => {
                          reloadStaffAttendance()
                          history.go(-1)
                        }}
                        data-qa="mark-departed-btn"
                      />
                    )}
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
