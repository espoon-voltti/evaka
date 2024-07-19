// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Timeline } from 'lib-common/generated/api-types/timeline'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import PageWrapper from 'lib-components/layout/PageWrapper'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faMagnifyingGlassMinus, faMagnifyingGlassPlus } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import TimelineGroup from './TimelineGroup'
import { hasRange, WithRange } from './common'
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
  const { personId } = useRouteParams(['personId'])
  const { i18n } = useTranslation()
  const timelineMaxRange = useMemo(
    () =>
      new FiniteDateRange(
        LocalDate.todayInHelsinkiTz().subMonths(15).withDate(1),
        LocalDate.todayInHelsinkiTz().addMonths(6).lastDayOfMonth()
      ),
    []
  )
  const timelineResult = useQueryResult(
    timelineQuery({
      personId,
      from: timelineMaxRange.start,
      to: timelineMaxRange.end
    })
  )

  return (
    <PageWrapper withReturn goBackLabel={i18n.common.goBack}>
      {renderResult(timelineResult, (timeline) => (
        <TimelineView timeline={timeline} timelineMaxRange={timelineMaxRange} />
      ))}
    </PageWrapper>
  )
})

const TimelineView = React.memo(function TimelineView({
  timeline,
  timelineMaxRange
}: {
  timeline: Timeline
  timelineMaxRange: FiniteDateRange
}) {
  const { i18n } = useTranslation()
  const [zoom, setZoom] = useState(20) // pixels / day
  const contentRange = getTimelineContentRange(timeline)
  const timelineRange = contentRange
    ? timelineMaxRange.intersection(contentRange)
    : timelineMaxRange

  if (!timelineRange) {
    Sentry.captureMessage(
      `Timeline content does not match requested range`,
      'error'
    )
    return null
  }

  return (
    <div>
      <H1>{i18n.timeline.title}</H1>
      <H2>
        {timeline.firstName} {timeline.lastName}
      </H2>
      <Gap size="s" />
      <FixedSpaceRow>
        <IconOnlyButton
          icon={faMagnifyingGlassPlus}
          aria-label="Zoom in"
          onClick={() => setZoom((prev) => Math.min(100, prev + 2))}
        />
        <IconOnlyButton
          icon={faMagnifyingGlassMinus}
          aria-label="Zoom out"
          onClick={() => setZoom((prev) => Math.max(1, prev - 2))}
        />
      </FixedSpaceRow>
      <Gap size="s" />

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
        <TimelineGroup
          data={timeline.feeDecisions.filter((d) => d.status === 'IGNORED')}
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
    </div>
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
  const allRanges: DateRange[] = []
  Object.values(t).forEach((value) => {
    if (Array.isArray(value)) {
      value.forEach((element) => {
        if (hasRange(element)) {
          allRanges.push(element.range)
        }
      })
    }
  })

  if (allRanges.length === 0) return null

  const spanningRange = allRanges.reduce((union, range) =>
    range.spanningRange(union)
  )

  // extended to from start of month to end of month
  return new DateRange(
    spanningRange.start.withDate(1),
    spanningRange.end ? spanningRange.end.lastDayOfMonth() : null
  )
}
