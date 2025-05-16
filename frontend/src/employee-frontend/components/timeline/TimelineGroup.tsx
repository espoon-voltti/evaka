// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type FiniteDateRange from 'lib-common/finite-date-range'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import TlLane from './TimelineLane'
import type { WithRange } from './common'
import type { EventRenderer } from './renderers'

export default function TimelineGroup<T extends WithRange>({
  data,
  renderer,
  timelineRange,
  zoom
}: {
  data: T[]
  renderer: EventRenderer<T>
  timelineRange: FiniteDateRange
  zoom: number
}) {
  const elemsWithinRange = data.filter((e) =>
    e.range.overlapsWith(timelineRange.asDateRange())
  )
  const lanes: T[][] = []
  elemsWithinRange.forEach((elem) => {
    let placed = false
    for (const laneElems of lanes) {
      if (!laneElems.some((other) => other.range.overlapsWith(elem.range))) {
        laneElems.push(elem)
        placed = true
        break
      }
    }
    if (!placed) {
      lanes.push([elem])
    }
  })

  return (
    <FixedSpaceColumn spacing="zero">
      {lanes.map((lane, i) => (
        <TlLane
          key={i}
          elements={lane}
          renderer={renderer}
          timelineRange={timelineRange}
          zoom={zoom}
        />
      ))}
    </FixedSpaceColumn>
  )
}
