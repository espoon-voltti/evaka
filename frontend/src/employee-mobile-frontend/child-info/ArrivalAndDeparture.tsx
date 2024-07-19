// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import { AttendanceTimes } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'
import { faArrowRotateLeft } from 'lib-icons'

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

  const dateInfo = (date: LocalDate) =>
    date.isEqual(LocalDate.todayInSystemTz())
      ? ''
      : date.isEqual(LocalDate.todayInSystemTz().subDays(1))
        ? i18n.common.yesterday
        : date.format('d.M.')

  const sortedAttendances = useMemo(
    () => sortBy(attendances, ({ arrived }) => arrived.formatIso()),
    [attendances]
  )

  return (
    <ArrivalTimeContainer>
      {sortedAttendances.map(({ arrived, departed }) => (
        <AttendanceRowContainer key={arrived.toSystemTzDate().toISOString()}>
          {arrived ? (
            <FixedSpaceRow justifyContent="center" alignItems="center">
              <ArrivalTime data-qa="arrival-time">
                <span>{i18n.attendances.arrivalTime}</span>
                <span>{`${dateInfo(arrived.toLocalDate())} ${arrived.toLocalTime().format()}`}</span>
              </ArrivalTime>
              {!departed && (
                <LegacyInlineButton
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
