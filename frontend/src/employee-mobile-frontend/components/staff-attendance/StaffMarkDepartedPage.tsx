// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft } from 'lib-icons'
import colors from 'lib-customizations/common'
import { formatTime, isValidTime } from 'lib-common/date'
import { Gap } from 'lib-components/white-space'
import InputField from 'lib-components/atoms/form/InputField'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import Title from 'lib-components/atoms/Title'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'
import { renderResult } from '../async-rendering'
import { TallContentArea } from '../mobile/components'
import { postStaffDeparture } from '../../api/staffAttendances'
import { Actions, BackButtonInline } from '../attendances/components'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { combine } from 'lib-common/api'
import { isAfter, parse } from 'date-fns'
import { UnitContext } from '../../state/unit'

export default React.memo(function StaffMarkDepartedPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitId, groupId, employeeId } = useParams<{
    unitId: string
    groupId: string
    employeeId: string
  }>()

  const { unitInfoResponse } = useContext(UnitContext)

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )
  useEffect(reloadStaffAttendance, [reloadStaffAttendance])

  const [pinCode, setPinCode] = useState('')
  const pinInputRef = useRef<HTMLInputElement>(null)
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
              return <ErrorSegment title={'Työntekijää ei löytynyt'} />

            const staffInfo = unitInfo.staff.find((s) => s.id === employeeId)
            const pinSet = staffInfo?.pinSet ?? true
            const pinLocked = staffInfo?.pinLocked || errorCode === 'PIN_LOCKED'

            return (
              <>
                <TimeWrapper>
                  <Title noMargin>Kirjaudu pin-koodilla</Title>
                  <InputField
                    inputRef={pinInputRef}
                    autoFocus
                    placeholder={i18n.attendances.pin.pinCode}
                    onChange={setPinCode}
                    value={pinCode}
                    width="s"
                    type="password"
                    data-qa="set-pin"
                    info={
                      errorCode === 'WRONG_PIN'
                        ? {
                            status: 'warning',
                            text: 'PIN-koodi ei kelvannut'
                          }
                        : undefined
                    }
                  />
                  <Gap />
                  <label>Lähtöaika</label>
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
                            text: `Oltava ${formatTime(
                              new Date()
                            )} tai aikaisemmin`
                          }
                        : undefined
                    }
                  />
                  <Gap />
                </TimeWrapper>
                <Gap size={'xs'} />
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
                          pinCode.length < 4
                        }
                        onClick={() =>
                          postStaffDeparture(attendanceId, {
                            time,
                            pinCode
                          }).then((res) => {
                            if (res.isFailure) {
                              setErrorCode(res.errorCode)
                              if (res.errorCode === 'WRONG_PIN') {
                                setPinCode('')
                                pinInputRef.current?.focus()
                              }
                            }
                            return res
                          })
                        }
                        onSuccess={() => history.go(-1)}
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

const TimeWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 20px;
  color: ${colors.blues.dark};
  font-weight: ${fontWeights.semibold};

  input {
    font-size: 60px;
    width: 100%;
    color: ${colors.blues.dark};
    font-family: Montserrat, sans-serif;
    font-weight: ${fontWeights.light};
    border-bottom: none;
  }
`
