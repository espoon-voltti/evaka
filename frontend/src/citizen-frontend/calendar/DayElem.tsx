import React from 'react'
import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { formatDate } from 'lib-common/date'
import LocalDate from 'lib-common/local-date'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { useTranslation } from '../localization'
import { DailyReservationData } from './api'

export interface DayProps {
  data: DailyReservationData
}

export default React.memo(function DayElem({
  data: { date, isHoliday, reservations }
}: DayProps) {
  const i18n = useTranslation()

  return (
    <DayDiv alignItems="center" today={date.isToday()} holiday={isHoliday}>
      <DayColumn spacing="xxs">
        <div>
          {i18n.common.datetime.weekdaysShort[date.getIsoDayOfWeek() - 1]}
        </div>
        <div>{date.format('dd.MM.')}</div>
      </DayColumn>
      <div>
        {reservations
          .map(
            (reservation) =>
              `${formatDate(reservation.startTime, 'HH:mm')} - ${formatDate(
                reservation.endTime,
                'HH:mm'
              )}`
          )
          .join(', ')}
      </div>
      {date.isBefore(LocalDate.today()) && <HistoryOverlay />}
    </DayDiv>
  )
})

const DayColumn = styled(FixedSpaceColumn)`
  width: 48px;
  color: ${colors.blues.dark};
  font-weight: 600;
  font-size: 16px;
`

const DayDiv = styled(FixedSpaceRow)<{ today: boolean; holiday: boolean }>`
  position: relative;
  padding: 8px 16px;
  height: 80px;
  border-bottom: 1px solid ${colors.greyscale.lighter};
  ${(p) =>
    p.today
      ? `
    border-left: 6px solid ${colors.brandEspoo.espooTurquoise};
    padding-left: 10px;
  `
      : ''}

  ${(p) =>
    p.holiday
      ? `
    border-left: 6px solid ${colors.accents.red};
    padding-left: 10px;
  `
      : ''}
`

const HistoryOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 99;
  opacity: 0.3;
  background-color: ${colors.blues.lighter};
`
