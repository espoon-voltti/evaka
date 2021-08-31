// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import LocalDate from 'lib-common/local-date'
import styled from 'styled-components'
import { useTranslation } from '../localization'
import colors from 'lib-customizations/common'
import { WeekProps } from './WeekElem'
import { H1 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { formatDate } from 'lib-common/date'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { faCalendarPlus } from 'lib-icons'

export interface Props {
  weeklyData: WeekProps[]
  onCreateReservationClicked: () => void
  selectDate: (date: LocalDate) => void
}

export default React.memo(function CalendarGridView({
  weeklyData,
  onCreateReservationClicked,
  selectDate
}: Props) {
  const i18n = useTranslation()

  return (
    <>
      <PageHeaderRow>
        <H1 noMargin>{i18n.calendar.title}</H1>
        <div>
          <InlineButton
            onClick={onCreateReservationClicked}
            text={'Tee varaus'}
            icon={faCalendarPlus}
          />
        </div>
      </PageHeaderRow>
      <CalendarHeader>
        <HeadingCell>{i18n.common.datetime.weekShort}</HeadingCell>
        {[0, 1, 2, 3, 4].map((d) => (
          <HeadingCell key={d}>
            {i18n.common.datetime.weekdaysShort[d]}
          </HeadingCell>
        ))}
      </CalendarHeader>
      <Grid>
        {weeklyData.map((w) => (
          <>
            <WeekNumber>{w.weekNumber}</WeekNumber>
            {w.dailyReservations.map((d) => (
              <DayCell
                key={d.date.formatIso()}
                today={d.date.isToday()}
                onClick={() => selectDate(d.date)}
              >
                <DayCellHeader>
                  <DayCellDate holiday={d.isHoliday}>
                    {d.date.format('d.M')}
                  </DayCellDate>
                </DayCellHeader>
                <DayCellReservations>
                  {d.reservations.length === 0 &&
                    (d.isHoliday ? (
                      <HolidayNote>{i18n.calendar.holiday}</HolidayNote>
                    ) : (
                      <NoReservation>Ei varausta</NoReservation>
                    ))}
                  {d.reservations
                    .map(
                      (reservation) =>
                        `${formatDate(
                          reservation.startTime,
                          'HH:mm'
                        )} - ${formatDate(reservation.endTime, 'HH:mm')}`
                    )
                    .join(', ')}
                </DayCellReservations>
                {d.date.isBefore(LocalDate.today()) && <HistoryOverlay />}
              </DayCell>
            ))}
          </>
        ))}
      </Grid>
    </>
  )
})

const PageHeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${defaultMargins.m};
`

const CalendarHeader = styled.div`
  display: grid;
  grid-template-columns: 40px repeat(5, 1fr);
`

const Grid = styled.div`
  display: grid;
  grid-template-columns: 40px repeat(5, 1fr);
`

const HeadingCell = styled.div`
  color: ${colors.blues.dark};
  font-family: 'Open Sans', sans-serif;
  font-style: normal;
  font-weight: 600;
  text-align: center;
  padding: ${defaultMargins.xs};
`

const WeekNumber = styled(HeadingCell)`
  padding-top: ${defaultMargins.s};
`

const DayCell = styled.div<{ today: boolean }>`
  position: relative;
  min-height: 150px;
  padding: ${defaultMargins.s};
  border-bottom: 1px solid ${colors.greyscale.lighter};
  border-right: 1px solid ${colors.greyscale.lighter};
  ${(p) =>
    p.today
      ? `
    border-left: 4px solid ${colors.brandEspoo.espooTurquoise};
    padding-left: calc(${defaultMargins.s} - 3px);
  `
      : `
    /* left border for second cell (first day cell) of each row */
    &:nth-child(6n+2) {
      border-left: 1px solid ${colors.greyscale.lighter};
    }
      `}

  /* top border for every day cell of first row */
  &:nth-child(-n + 7) {
    border-top: 1px solid ${colors.greyscale.lighter};
  }
`

const DayCellHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.m};
`

const DayCellDate = styled.div<{ holiday: boolean }>`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  color: ${(p) => (p.holiday ? colors.greyscale.dark : colors.blues.dark)};
  font-weight: 600;
  font-size: 1.25rem;
`

const DayCellReservations = styled.div``

const HistoryOverlay = styled.div`
  position: absolute;
  top: 1px;
  left: 1px;
  width: calc(100% - 2px);
  height: calc(100% - 2px);
  z-index: 1;
  opacity: 0.6;
  background-color: ${colors.greyscale.white};
`

const HolidayNote = styled.div`
  font-style: italic;
  color: ${colors.greyscale.dark};
`

const NoReservation = styled.span`
  color: ${colors.accents.orangeDark};
`
