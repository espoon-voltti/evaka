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

  const arrival = child.attendance?.arrived
  const departure = child.attendance?.departed

  if (!arrival) {
    return null
  }

  const arrivalDate = arrival.toLocalDate()
  const dateInfo = arrivalDate.isEqual(LocalDate.todayInSystemTz())
    ? ''
    : arrivalDate.isEqual(LocalDate.todayInSystemTz().subDays(1))
    ? i18n.common.yesterday
    : arrivalDate.format('d.M.')

  return (
    <FixedSpaceRow justifyContent="center">
      {arrival ? (
        <ArrivalTimeContainer>
          <ArrivalTime>
            <span>{i18n.attendances.arrivalTime}</span>
            <span>{`${dateInfo} ${arrival.toLocalTime().format()}`}</span>
          </ArrivalTime>
          {!departure && (
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
        </ArrivalTimeContainer>
      ) : null}
      {departure ? (
        <ArrivalTime>
          <span>{i18n.attendances.departureTime}</span>
          <span>{departure.toLocalTime().format()}</span>
        </ArrivalTime>
      ) : null}
    </FixedSpaceRow>
  )
})

const ArrivalTimeContainer = styled.div`
  display: flex;
  align-items: center;
`

const InlineWideAsyncButton = styled(AsyncButton)`
  border: none;
`
