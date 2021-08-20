import React from 'react'
import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { DailyReservationData } from './api'
import DayElem from './DayElem'

export interface WeekProps {
  weekNumber: number
  data: DailyReservationData[]
}

export default React.memo(function WeekElem({ weekNumber, data }: WeekProps) {
  return (
    <div>
      <WeekDiv>Vk {weekNumber}</WeekDiv>
      <div>
        {data.map((d) => (
          <DayElem data={d} key={d.date.formatIso()} />
        ))}
      </div>
    </div>
  )
})

const WeekDiv = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0 8px;
  background-color: ${colors.blues.lighter};
  color: ${colors.blues.dark};
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid ${colors.greyscale.lighter};
`
