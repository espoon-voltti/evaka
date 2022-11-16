// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Child } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faArrowRotateLeft } from 'lib-icons'

import { returnToComing } from '../../../api/attendances'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { ArrivalTime } from '../components'

interface Props {
  child: Child
  unitId: string
}

export default React.memo(function ArrivalAndDeparture({
  child,
  unitId
}: Props) {
  const { i18n } = useTranslation()
  const { reloadAttendances } = useContext(ChildAttendanceContext)
  const navigate = useNavigate()

  function returnToComingCall() {
    return returnToComing(unitId, child.id)
  }

  const latestArrival =
    child.attendances.length > 0 ? child.attendances[0].arrived : null
  //const latestDeparture = child.attendances.length > 0 ? child.attendances[0].departed : null

  if (!latestArrival) {
    return null
  }

  const arrivalDate = latestArrival.toLocalDate()
  const dateInfo = arrivalDate.isEqual(LocalDate.todayInSystemTz())
    ? ''
    : arrivalDate.isEqual(LocalDate.todayInSystemTz().subDays(1))
    ? i18n.common.yesterday
    : arrivalDate.format('d.M.')

  return (
    <ArrivalTimeContainer>
      {child.attendances.reverse().map(({ arrived, departed }) => (
        <AttendanceRowContainer key={arrived.toSystemTzDate().toISOString()}>
          {arrived ? (
            <FixedSpaceRow justifyContent="center" alignItems="center">
              <ArrivalTime>
                <span>{i18n.attendances.arrivalTime}</span>
                <span>{`${dateInfo} ${arrived.toLocalTime().format()}`}</span>
              </ArrivalTime>
              {!departed && (
                <InlineWideAsyncButton
                  text={i18n.common.cancel}
                  icon={faArrowRotateLeft}
                  onClick={() => returnToComingCall()}
                  onSuccess={() => {
                    reloadAttendances()
                    navigate(-1)
                  }}
                  data-qa="cancel-arrival-button"
                />
              )}
            </FixedSpaceRow>
          ) : null}
          {departed ? (
            <ArrivalTime>
              <span>{i18n.attendances.departureTime}</span>
              <span>{departed.toLocalTime().format()}</span>
            </ArrivalTime>
          ) : null}
        </AttendanceRowContainer>
      ))}
    </ArrivalTimeContainer>
  )
})

const ArrivalTimeContainer = styled.div`
  display: flex;
  align-items: center;
  flex-direction: column;
`

const InlineWideAsyncButton = styled(AsyncButton)`
  border: none;
`
const AttendanceRowContainer = styled.div`
  display: flex;
  flex-direction: row;
  width: 100%;
  justify-content: space-evenly;
  align-items: center;
`
