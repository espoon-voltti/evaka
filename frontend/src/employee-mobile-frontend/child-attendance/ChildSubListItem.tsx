// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import { getTodaysServiceTimes } from 'employee-mobile-frontend/common/dailyServiceTimes'
import LocalDate from 'lib-common/local-date'
import { reservationHasTimes } from 'lib-common/reservations'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { Bold } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../common/i18n'

import { CategorizedReservationInfo } from './ChildReservationList'

const ChildBoxInfo = styled.div`
  margin-left: 24px;
  flex-grow: 1;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const DetailsRow = styled.div`
  flex-direction: column;
  justify-content: space-between;
  align-items: center;

  font-size: 0.875em;
  width: 100%;

  &.absent > span {
    color: ${colors.grayscale.g70};
  }
`

const NameRow = styled.div`
  width: 100%;
  word-break: break-word;
  margin-right: 10px;

  &.absent > span {
    color: ${colors.grayscale.g70};
  }

  min-height: 40px;
`

const ChildBox = styled.div`
  align-items: center;
  min-height: 40px;
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.grayscale.g0};
`

export const ChevronBox = styled.div`
  min-width: 32px;
  padding: 0;
`

export const DateBox = styled.div`
  min-width: 70px;
`

const AgeRoundIcon = styled(RoundIcon)`
  &.m {
    font-size: 14px;
  }
`

const Placeholder = styled.span`
  display: inline-block;
  vertical-align: middle;
  height: 100%;
`

const Name = styled(Bold)`
  display: inline-block;
  vertical-align: middle;
`

const ReservationText = styled.span`
  display: inline-block;
  vertical-align: middle;
  width: 100%;
`

interface ChildSubListItemProps {
  reservationData: CategorizedReservationInfo
  date: LocalDate
}

const timeFormat = 'HH:mm'

export default React.memo(function ChildSubListItem({
  reservationData,
  date
}: ChildSubListItemProps) {
  const { i18n } = useTranslation()

  const childAge = date.differenceInYears(reservationData.dateOfBirth)

  const reservationTextContent = useMemo(() => {
    if (reservationData.scheduleType === 'TERM_BREAK')
      return [i18n.attendances.confirmedDays.status.ON_TERM_BREAK]
    if (reservationData.absent)
      return [i18n.attendances.confirmedDays.status.ABSENT]
    if (reservationData.outOnBackupPlacement)
      return [i18n.attendances.confirmedDays.inOtherUnit]

    const [withTimes] = partition(
      reservationData.reservations,
      reservationHasTimes
    )

    if (withTimes.length > 0)
      return withTimes.map(
        (r) =>
          `${r.startTime.format(timeFormat)} - ${r.endTime.format(timeFormat)}`
      )

    if (reservationData.isInHolidayPeriod) {
      if (reservationData.reservations.length === 0)
        return [i18n.attendances.confirmedDays.noHolidayReservation]
    }

    if (reservationData.scheduleType === 'FIXED_SCHEDULE') {
      return [i18n.attendances.serviceTime.present]
    }

    const todaysServiceTime = getTodaysServiceTimes(
      reservationData.dailyServiceTimes
    )

    return [
      todaysServiceTime === 'not_set'
        ? i18n.attendances.serviceTime.notSetShort
        : todaysServiceTime === 'not_today'
          ? i18n.attendances.serviceTime.noServiceTodayShort
          : todaysServiceTime === 'variable_times'
            ? i18n.attendances.serviceTime.variableTimesShort
            : `${todaysServiceTime.start.format(
                timeFormat
              )} - ${todaysServiceTime.end.format(timeFormat)} (s)`
    ]
  }, [reservationData, i18n])

  return (
    <ChildBox
      data-qa={`child-${reservationData.childId}`}
      className={reservationData.sortCategory > 3 ? 'absent' : 'present'}
    >
      <AgeRoundIcon
        content={`${childAge}v`}
        color={
          childAge < 3
            ? reservationData.sortCategory > 3
              ? colors.grayscale.g35
              : colors.accents.a6turquoise
            : reservationData.sortCategory > 3
              ? colors.grayscale.g70
              : colors.main.m1
        }
        size="m"
      />
      <ChildBoxInfo>
        <NameRow
          className={reservationData.sortCategory > 3 ? 'absent' : 'present'}
        >
          <Name data-qa="child-name">
            {reservationData.firstName.split(/\s/)[0]}{' '}
            {reservationData.lastName}{' '}
            {reservationData.preferredName
              ? ` (${reservationData.preferredName})`
              : null}
          </Name>
          <Placeholder />
        </NameRow>
        <DetailsRow
          className={reservationData.sortCategory > 3 ? 'absent' : 'present'}
        >
          {reservationTextContent.length > 1 ? (
            reservationTextContent.map((res, index) => (
              <ReservationText
                data-qa={`reservation-content-${index}`}
                key={`${reservationData.childId}-${res}-${index}`}
              >
                {res}
              </ReservationText>
            ))
          ) : (
            <>
              <ReservationText data-qa="reservation-content-0">
                {reservationTextContent[0]}
              </ReservationText>
              <Placeholder />
            </>
          )}
        </DetailsRow>
      </ChildBoxInfo>
    </ChildBox>
  )
})
