// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faMagnifyingGlassMinus, faMagnifyingGlassPlus } from 'Icons'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Timeline } from 'lib-common/generated/api-types/timeline'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import TimelineGroup from './TimelineGroup'
import { WithRange } from './common'
import { timelineQuery } from './queries'
import {
  childRenderer,
  feeDecisionRenderer,
  incomeRenderer,
  monthRenderer,
  partnerRenderer,
  valueDecisionRenderer
} from './renderers'

const TlContainer = styled.div`
  width: 100%;
  min-height: 500px;
  overflow-x: scroll;
  display: flex;
  flex-direction: column;
`

export default React.memo(function TimelinePage() {
  const { personId } = useNonNullableParams()
  const { i18n } = useTranslation()
  const timelineMaxRange = useMemo(
    () =>
      new FiniteDateRange(
        LocalDate.todayInHelsinkiTz().subMonths(15).withDate(1),
        LocalDate.todayInHelsinkiTz().addMonths(7).withDate(1)
      ),
    []
  )
  const timelineResult = useQueryResult(
    timelineQuery({ personId, range: timelineMaxRange })
  )
  const [zoom, setZoom] = useState(20) // pixels / day

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.timeline.title}</H1>
        {timelineResult.isSuccess && (
          <H2>
            {timelineResult.value.firstName} {timelineResult.value.lastName}
          </H2>
        )}
        <Gap size="s" />
        <FixedSpaceRow>
          <IconButton
            icon={faMagnifyingGlassPlus}
            aria-label="Zoom in"
            onClick={() => setZoom((prev) => Math.min(100, prev + 2))}
          />
          <IconButton
            icon={faMagnifyingGlassMinus}
            aria-label="Zoom out"
            onClick={() => setZoom((prev) => Math.max(1, prev - 2))}
          />
        </FixedSpaceRow>
        <Gap size="s" />
        {renderResult(timelineResult, (timeline) => (
          <TimelineView
            timeline={timeline}
            zoom={zoom}
            timelineMaxRange={timelineMaxRange}
          />
        ))}
      </ContentArea>
    </Container>
  )
})

const TimelineView = React.memo(function TimelineView({
  timeline,
  zoom,
  timelineMaxRange
}: {
  timeline: Timeline
  zoom: number
  timelineMaxRange: FiniteDateRange
}) {
  const contentRange = getTimelineContentRange(timeline)
  const timelineRange = contentRange
    ? timelineMaxRange.intersection(contentRange)
    : timelineMaxRange

  if (!timelineRange) {
    console.warn('Content does not match requested range')
    return null
  }

  return (
    <TlContainer>
      {/*Months row*/}
      <TimelineGroup
        data={getMonthRanges(timelineRange)}
        renderer={monthRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />

      <Gap size="s" />

      {/*Fee decisions grouped by statuses*/}
      <TimelineGroup
        data={timeline.feeDecisions.filter((d) => d.status === 'SENT')}
        renderer={feeDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />
      <TimelineGroup
        data={timeline.feeDecisions.filter((d) =>
          ['WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING'].includes(
            d.status
          )
        )}
        renderer={feeDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />
      <TimelineGroup
        data={timeline.feeDecisions.filter((d) => d.status === 'DRAFT')}
        renderer={feeDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />
      <TimelineGroup
        data={timeline.feeDecisions.filter((d) => d.status === 'ANNULLED')}
        renderer={feeDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />

      <Gap size="xs" />

      {/*Value decisions grouped by statuses*/}
      <TimelineGroup
        data={timeline.valueDecisions.filter((d) => d.status === 'SENT')}
        renderer={valueDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />
      <TimelineGroup
        data={timeline.valueDecisions.filter((d) =>
          ['WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING'].includes(
            d.status
          )
        )}
        renderer={valueDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />
      <TimelineGroup
        data={timeline.valueDecisions.filter((d) => d.status === 'DRAFT')}
        renderer={valueDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />
      <TimelineGroup
        data={timeline.valueDecisions.filter((d) => d.status === 'ANNULLED')}
        renderer={valueDecisionRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />

      <Gap size="xs" />

      {/*Incomes*/}
      <TimelineGroup
        data={timeline.incomes}
        renderer={incomeRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />

      <Gap size="xs" />

      {/*PARTNERS*/}
      <TimelineGroup
        data={timeline.partners}
        renderer={partnerRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />

      <Gap size="xs" />

      {/*PARTNERS*/}
      <TimelineGroup
        data={timeline.children}
        renderer={childRenderer}
        timelineRange={timelineRange}
        zoom={zoom}
      />

      <Gap size="X5L" />
    </TlContainer>
  )
})

const getMonthRanges = (timelineRange: FiniteDateRange): WithRange[] => {
  const months: WithRange[] = []
  let start = timelineRange.start
  while (timelineRange.includes(start)) {
    const monthEnd = start.addMonths(1).withDate(1).subDays(1)
    const end = timelineRange.includes(monthEnd) ? monthEnd : timelineRange.end
    months.push({
      range: new DateRange(start, end)
    })
    start = start.addMonths(1)
  }
  return months
}

const getTimelineContentRange = (t: Timeline): DateRange | null => {
  const allRanges: DateRange[] = [
    ...t.feeDecisions,
    ...t.incomes,
    ...t.partners,
    ...t.children
  ].map((event) => event.range)

  if (allRanges.length === 0) return null

  return allRanges.reduce((union, range) => range.spanningRange(union))
}
