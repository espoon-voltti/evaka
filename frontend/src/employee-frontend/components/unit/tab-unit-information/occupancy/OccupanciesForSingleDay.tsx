// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { fontWeights } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import {
  unitOccupanciesQuery,
  unitPlannedOccupanciesForDayQuery,
  unitRealizedOccupanciesForDayQuery
} from '../../queries'

import OccupancyDayGraphPlanned from './OccupancyDayGraphPlanned'
import OccupancyDayGraphRealized from './OccupancyDayGraphRealized'

const Container = styled.div`
  text-align: center;
`

const Value = styled.div`
  font-weight: ${fontWeights.semibold};
  font-size: 1.2rem;
  margin-bottom: 30px;
`

export const LegendSquare = styled.div<{ color: string; small?: boolean }>`
  height: ${(p) => (p.small ? '10px' : '20px')};
  width: ${(p) => (p.small ? '10px' : '20px')};
  background-color: ${(p) => p.color};
  border-radius: 2px;
`

export const RealtimeRealizedOccupanciesForSingleDay = React.memo(
  function RealtimeRealizedOccupanciesForSingleDay({
    unitId,
    groupId,
    date,
    shiftCareUnit
  }: {
    unitId: UUID
    groupId: UUID | null
    date: LocalDate
    shiftCareUnit: boolean
  }) {
    const occupancies = useQueryResult(
      unitRealizedOccupanciesForDayQuery({ unitId, date, groupId })
    )

    return renderResult(occupancies, (occupancies, isReloading) => (
      <div data-qa="unit-attendances" data-isloading={isReloading}>
        <OccupancyDayGraphRealized
          queryDate={date}
          occupancy={occupancies}
          shiftCareUnit={shiftCareUnit}
        />
      </div>
    ))
  }
)

export const RealtimePlannedOccupanciesForSingleDay = React.memo(
  function RealtimePlannedOccupanciesForSingleDay({
    unitId,
    groupId,
    date
  }: {
    unitId: UUID
    groupId: UUID | null
    date: LocalDate
  }) {
    const rows = useQueryResult(
      unitPlannedOccupanciesForDayQuery({ unitId, date, groupId })
    )

    return renderResult(rows, (rows, isReloading) => (
      <div data-qa="unit-attendances" data-isloading={isReloading}>
        <OccupancyDayGraphPlanned rows={rows} />
      </div>
    ))
  }
)

export const SimpleOccupanciesForSingleDay = React.memo(
  function SimpleOccupanciesForSingleDay({
    unitId,
    groupId,
    date
  }: {
    unitId: UUID
    groupId: UUID | null
    date: LocalDate
  }) {
    const { i18n } = useTranslation()

    const occupancies = useQueryResult(
      unitOccupanciesQuery({ unitId, from: date, to: date, groupId })
    )

    return renderResult(occupancies, (occupancies, isReloading) => {
      const { confirmed, planned, realized } = occupancies
      return (
        <Container data-qa="unit-attendances" data-isloading={isReloading}>
          <Title size={4}>{i18n.unit.occupancy.subtitles.confirmed}</Title>
          {confirmed.occupancies.length > 0 &&
          confirmed.occupancies[0].percentage != null ? (
            <Value>{`${confirmed.occupancies[0].percentage} %`}</Value>
          ) : (
            <div data-qa="occupancies-no-valid-values-confirmed">
              {i18n.unit.occupancy.noValidValues}
            </div>
          )}
          <Title size={4}>{i18n.unit.occupancy.subtitles.planned}</Title>
          {planned.occupancies.length > 0 &&
          planned.occupancies[0].percentage != null ? (
            <Value>{`${planned.occupancies[0].percentage} %`}</Value>
          ) : (
            <div data-qa="occupancies-no-valid-values-planned">
              {i18n.unit.occupancy.noValidValues}
            </div>
          )}
          <Title size={4}>{i18n.unit.occupancy.subtitles.realized}</Title>
          {realized.occupancies.length > 0 &&
          realized.occupancies[0].percentage != null ? (
            <Value>{`${realized.occupancies[0].percentage} %`}</Value>
          ) : (
            <div data-qa="occupancies-no-valid-values-realized">
              {i18n.unit.occupancy.noValidValuesRealized}
            </div>
          )}
        </Container>
      )
    })
  }
)
