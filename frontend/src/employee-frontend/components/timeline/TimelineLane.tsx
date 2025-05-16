// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type DateRange from 'lib-common/date-range'
import type FiniteDateRange from 'lib-common/finite-date-range'
import { maxOf, minOf } from 'lib-common/ordered'

import TlEvent from './TimelineEvent'
import type { WithRange } from './common'
import type { EventRenderer } from './renderers'

const TlLaneWrapper = styled.div`
  width: 100%;
  display: flex;
`

export default function TlLane<T extends WithRange>({
  elements,
  renderer,
  timelineRange,
  zoom
}: {
  elements: T[]
  renderer: EventRenderer<T>
  timelineRange: FiniteDateRange
  zoom: number
}) {
  const positionedElems = elements.reduce<
    { width: number; relLeft: number; elem: T }[]
  >((acc, elem) => {
    const absLeft = getElementLeft(elem.range, timelineRange, zoom)
    const absRight = getElementRight(elem.range, timelineRange, zoom)
    const prevWidthsSum = acc.reduce((sum, { width }) => sum + width, 0)
    acc.push({
      width: absRight - absLeft,
      relLeft: absLeft - prevWidthsSum,
      elem
    })
    return acc
  }, [])

  return (
    <TlLaneWrapper>
      {positionedElems.map(({ width, relLeft, elem }, i) => (
        <TlEvent
          key={i}
          event={elem}
          renderer={renderer}
          left={relLeft}
          width={width}
          timelineRange={timelineRange}
          zoom={zoom}
        />
      ))}
    </TlLaneWrapper>
  )
}

const getElementLeft = (
  range: DateRange,
  timelineRange: FiniteDateRange,
  zoom: number
) =>
  maxOf(range.start, timelineRange.start).differenceInDays(
    timelineRange.start
  ) * zoom

const getElementRight = (
  range: DateRange,
  timelineRange: FiniteDateRange,
  zoom: number
) =>
  (minOf(range.end || timelineRange.end, timelineRange.end).differenceInDays(
    timelineRange.start
  ) +
    1) *
  zoom
