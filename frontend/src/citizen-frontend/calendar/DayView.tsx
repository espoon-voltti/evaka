// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useMemo } from 'react'
import styled from 'styled-components'
import { faChevronLeft, faChevronRight } from 'lib-icons'
import { DATE_FORMAT_TIME_ONLY, formatDate } from 'lib-common/date'
import LocalDate from 'lib-common/local-date'
import { H1, H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { useTranslation } from 'citizen-frontend/localization'
import { ReservationsResponse } from './api'
import CalendarModal from './CalendarModal'

interface Props {
  date: LocalDate
  data: ReservationsResponse
  selectDate: (date: LocalDate) => void
}

export default React.memo(function DayView({ date, data, selectDate }: Props) {
  const i18n = useTranslation()

  const childrenWithReservations = useMemo(() => {
    const reservations = data.dailyData.find((reservation) =>
      date.isEqual(reservation.date)
    )

    return data.children.map((child) => {
      const reservation = reservations?.reservations.find(
        ({ childId }) => childId === child.id
      )

      return {
        child,
        reservation
      }
    })
  }, [date, data])

  return (
    <CalendarModal highlight={date.isEqual(LocalDate.today())}>
      <DayPicker>
        <IconButton
          icon={faChevronLeft}
          onClick={() => selectDate(date.subDays(1))}
        />
        <DayOfWeek>{`${
          i18n.common.datetime.weekdays[date.getIsoDayOfWeek() - 1]
        } ${date.format()}`}</DayOfWeek>
        <IconButton
          icon={faChevronRight}
          onClick={() => selectDate(date.addDays(1))}
        />
      </DayPicker>
      <Gap size="m" />
      <H2 noMargin>Varaukset ja toteuma</H2>
      <Gap size="s" />
      {childrenWithReservations.map(({ child, reservation }, index) => (
        <Fragment key={child.id}>
          {index !== 0 ? <Separator /> : null}
          <H3 noMargin>
            {child.preferredName || child.firstName.split(' ')[0]}
          </H3>
          <Gap size="s" />
          <Grid>
            <Label>Varaus</Label>
            {reservation === undefined ? (
              <NoReservation>Ei varausta</NoReservation>
            ) : (
              <span>{`${formatDate(
                reservation.startTime,
                DATE_FORMAT_TIME_ONLY
              )} – ${formatDate(
                reservation.endTime,
                DATE_FORMAT_TIME_ONLY
              )}`}</span>
            )}
            <Label>Toteuma</Label>
            <span>–</span>
          </Grid>
        </Fragment>
      ))}
    </CalendarModal>
  )
})

const DayPicker = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
`

const DayOfWeek = styled(H1)`
  margin: 0 ${defaultMargins.s};
  text-align: center;
`

const Grid = styled.div`
  display: grid;
  grid-template-columns: min-content auto;
  row-gap: ${defaultMargins.xs};
  column-gap: ${defaultMargins.s};
`

const NoReservation = styled.span`
  color: ${(p) => p.theme.colors.accents.orangeDark};
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.greyscale.lighter};
  margin: ${defaultMargins.s} 0;
`
