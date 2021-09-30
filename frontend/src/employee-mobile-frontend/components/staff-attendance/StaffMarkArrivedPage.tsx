// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft } from 'lib-icons'
import colors from 'lib-customizations/common'
import { formatTime, isValidTime } from 'lib-common/date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Gap } from 'lib-components/white-space'
import InputField from 'lib-components/atoms/form/InputField'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import Title from 'lib-components/atoms/Title'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'
import { TallContentArea } from '../mobile/components'
import { Loading, Result } from 'lib-common/api'
import { StaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import {
  getUnitStaffAttendances,
  postStaffArrival
} from '../../api/staffAttendances2'
import { Actions, BackButtonInline } from '../attendances/components'
import { useTranslation } from '../../state/i18n'
import { renderResult } from 'lib-components/async-rendering'

export default React.memo(function StaffMarkArrivedPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitId, employeeId } = useParams<{
    unitId: string
    employeeId: string
  }>()

  const [attendanceResponse, setAttendanceResponse] = useState<
    Result<StaffAttendanceResponse>
  >(Loading.of())
  const loadAttendances = useRestApi(
    getUnitStaffAttendances,
    setAttendanceResponse
  )
  useEffect(() => loadAttendances(unitId), [unitId, loadAttendances])

  const staffMember = attendanceResponse.map((res) =>
    res.staff.find((s) => s.employeeId === employeeId)
  )

  const [pinCode, setPinCode] = useState('')
  const [time, setTime] = useState<string>(formatTime(new Date()))

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal={'zero'}
      paddingVertical={'zero'}
    >
      <div>
        <BackButtonInline
          onClick={() =>
            history.replace(`/units/${unitId}/staff2/${employeeId}`)
          }
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
                    disabled={!isValidTime(time) || pinCode.length < 4}
                    onClick={() =>
                      postStaffArrival(unitId, {
                        employeeId,
                        time,
                        pinCode
                      })
                    }
                    onSuccess={() => history.go(-2)}
                    data-qa="mark-present-btn"
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
