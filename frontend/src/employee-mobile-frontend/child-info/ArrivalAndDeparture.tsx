// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faArrowRotateLeft } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import { AttendanceTimes } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'

import { ArrivalTime } from '../common/components'
import { useTranslation } from '../common/i18n'

interface Props {
  attendances: AttendanceTimes[]
  returnToComing: () => void
}

export default React.memo(function ArrivalAndDeparture({
  attendances,
  returnToComing
}: Props) {
  const { i18n } = useTranslation()

  const latestArrival = attendances.length > 0 ? attendances[0].arrived : null

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
      {attendances.reverse().map(({ arrived, departed }) => (
        <AttendanceRowContainer key={arrived.toSystemTzDate().toISOString()}>
          {arrived ? (
            <FixedSpaceRow justifyContent="center" alignItems="center">
              <ArrivalTime data-qa="arrival-time">
                <span>{i18n.attendances.arrivalTime}</span>
                <span>{`${dateInfo} ${arrived.toLocalTime().format()}`}</span>
              </ArrivalTime>
              {!departed && (
                <InlineButton
                  icon={faArrowRotateLeft}
                  text={i18n.common.cancel}
                  onClick={returnToComing}
                  data-qa="cancel-arrival-button"
                  color={colors.main.m2}
                />
              )}
            </FixedSpaceRow>
          ) : null}
          {departed ? (
            <ArrivalTime data-qa="departure-time">
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

const AttendanceRowContainer = styled.div`
  display: flex;
  flex-direction: row;
  width: 100%;
  justify-content: space-evenly;
  align-items: center;
`
