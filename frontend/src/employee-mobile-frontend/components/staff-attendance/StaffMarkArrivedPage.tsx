// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
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
import { postStaffArrival } from '../../api/staffAttendances'
import { Actions, BackButtonInline } from '../attendances/components'
import { useTranslation } from '../../state/i18n'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import { UUID } from 'lib-common/types'
import { UnitContext } from '../../state/unit'
import { combine, Success } from 'lib-common/api'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'

export default React.memo(function StaffMarkArrivedPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { groupId, employeeId } = useParams<{
    groupId: UUID | 'all'
    employeeId: UUID
  }>()

  const { unitInfoResponse } = useContext(UnitContext)

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )
  useEffect(reloadStaffAttendance, [reloadStaffAttendance])

  const staffMember = staffAttendanceResponse.map((res) =>
    res.staff.find((s) => s.employeeId === employeeId)
  )

  const [pinCode, setPinCode] = useState('')
  const [time, setTime] = useState<string>(formatTime(new Date()))
  const [attendanceGroup, setAttendanceGroup] = useState<UUID | undefined>(
    groupId !== 'all' ? groupId : undefined
  )

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
      attendanceGroup === null &&
      groupOptions.isSuccess &&
      groupOptions.value.length === 1
    ) {
      setAttendanceGroup(groupOptions.value[0].value)
    }
  }, [attendanceGroup, groupOptions])

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
        {renderResult(staffMember, (staffMember) =>
          staffMember === undefined ? (
            <ErrorSegment title={'Työntekijää ei löytynyt'} />
          ) : (
            <>
              <TimeWrapper>
                <Title noMargin>Kirjaudu pin-koodilla</Title>
                <InputField
                  autoFocus
                  placeholder={i18n.attendances.pin.pinCode}
                  onChange={setPinCode}
                  value={pinCode}
                  width="s"
                  type="password"
                  data-qa="set-pin"
                />
                <Gap />
                <label>Saapumisaika</label>
                <InputField
                  onChange={setTime}
                  value={time}
                  width="s"
                  type="time"
                  data-qa="set-time"
                />
                {renderResult(groupOptions, (groupOptions) =>
                  groupOptions.length > 1 ? (
                    <>
                      <Gap />
                      <label>Ryhmä</label>
                      <SimpleSelect
                        value={attendanceGroup}
                        placeholder={'Valitse ryhmä'}
                        options={groupOptions}
                        onChange={(e) => setAttendanceGroup(e.target.value)}
                      />
                    </>
                  ) : null
                )}
                <Gap />
              </TimeWrapper>
              <Gap size={'xs'} />
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
                      !isValidTime(time) ||
                      pinCode.length < 4 ||
                      !attendanceGroup
                    }
                    onClick={() =>
                      attendanceGroup
                        ? postStaffArrival({
                            employeeId,
                            groupId: attendanceGroup,
                            time,
                            pinCode
                          })
                        : Promise.reject()
                    }
                    onSuccess={() => history.go(-2)}
                    data-qa="mark-arrived-btn"
                  />
                </FixedSpaceRow>
              </Actions>
            </>
          )
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
