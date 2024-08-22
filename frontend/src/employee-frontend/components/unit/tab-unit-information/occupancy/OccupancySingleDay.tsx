// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import {
  OccupancyResponse,
  RealtimeOccupancy
} from 'lib-common/generated/api-types/occupancy'
import LocalDate from 'lib-common/local-date'
import Title from 'lib-components/atoms/Title'
import { fontWeights } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'

import OccupancyDayGraph from './OccupancyDayGraph'

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

interface Props {
  queryDate: LocalDate
  occupancies: OccupancyResponse
  plannedOccupancies: OccupancyResponse
  realizedOccupancies: OccupancyResponse
  realtimeOccupancies: RealtimeOccupancy | null
  shiftCareUnit: boolean
}

const OccupancySingleDay = React.memo(function OccupancySingleDay({
  queryDate,
  occupancies,
  plannedOccupancies,
  realizedOccupancies,
  realtimeOccupancies,
  shiftCareUnit
}: Props) {
  const { i18n } = useTranslation()

  if (realtimeOccupancies) {
    return (
      <OccupancyDayGraph
        queryDate={queryDate}
        occupancy={realtimeOccupancies}
        shiftCareUnit={shiftCareUnit}
      />
    )
  }

  return (
    <Container>
      <Title size={4}>{i18n.unit.occupancy.subtitles.confirmed}</Title>
      {occupancies.occupancies.length > 0 &&
      occupancies.occupancies[0].percentage != null ? (
        <Value>{`${occupancies.occupancies[0].percentage} %`}</Value>
      ) : (
        <div data-qa="occupancies-no-valid-values-confirmed">
          {i18n.unit.occupancy.noValidValues}
        </div>
      )}
      <Title size={4}>{i18n.unit.occupancy.subtitles.planned}</Title>
      {plannedOccupancies.occupancies.length > 0 &&
      plannedOccupancies.occupancies[0].percentage != null ? (
        <Value>{`${plannedOccupancies.occupancies[0].percentage} %`}</Value>
      ) : (
        <div data-qa="occupancies-no-valid-values-planned">
          {i18n.unit.occupancy.noValidValues}
        </div>
      )}
      <Title size={4}>{i18n.unit.occupancy.subtitles.realized}</Title>
      {realizedOccupancies.occupancies.length > 0 &&
      realizedOccupancies.occupancies[0].percentage != null ? (
        <Value>{`${realizedOccupancies.occupancies[0].percentage} %`}</Value>
      ) : (
        <div data-qa="occupancies-no-valid-values-realized">
          {i18n.unit.occupancy.noValidValuesRealized}
        </div>
      )}
    </Container>
  )
})

export default OccupancySingleDay
