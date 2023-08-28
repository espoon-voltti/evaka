// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// TODO: Refactor to components?
/* eslint react-hooks/rules-of-hooks: 0 */

import React from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  TimelineChildDetailed,
  TimelineFeeDecision,
  TimelineIncome,
  TimelinePartnerDetailed,
  TimelinePlacement,
  TimelineServiceNeed
} from 'lib-common/generated/api-types/timeline'
import { formatCents } from 'lib-common/money'
import { maxOf, minOf } from 'lib-common/ordered'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import TimelineGroup from './TimelineGroup'
import { WithRange } from './common'

type SummaryRenderer<T extends WithRange> = (elem: T) => string
type TooltipRenderer<T extends WithRange> = (elem: T) => React.ReactNode
type NestedContentRenderer<T extends WithRange> = (
  elem: T,
  timelineRange: FiniteDateRange,
  zoom: number
) => React.ReactNode
export interface EventRenderer<T extends WithRange> {
  color: (elem: T) => string
  summary: SummaryRenderer<T>
  linkProvider?: (elem: T) => string
  tooltip?: TooltipRenderer<T>
  nestedContent?: NestedContentRenderer<T>
}

const TlNestedContainer = styled.div`
  width: 100%;
  min-height: 30px;
  display: flex;
  flex-direction: column;
`

export const monthRenderer: EventRenderer<WithRange> = {
  color: () => '#ffffff',
  summary: (m: WithRange) => m.range.start.format('MM/yyyy')
}

export const feeDecisionRenderer: EventRenderer<TimelineFeeDecision> = {
  color: ({ status }) => {
    switch (status) {
      case 'SENT':
      case 'WAITING_FOR_SENDING':
      case 'WAITING_FOR_MANUAL_SENDING':
        return '#9999ff'
      case 'DRAFT':
        return '#88aadd'
      case 'ANNULLED':
        return '#9999bb'
    }
  },
  summary: (d: TimelineFeeDecision) => {
    const { i18n } = useTranslation()
    return `Maksupäätös ${i18n.feeDecision.status[d.status]}`
  },
  linkProvider: (elem) => `/finance/fee-decisions/${elem.id}`,
  tooltip: (d: TimelineFeeDecision) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{d.range.format()}</span>
        <span>{i18n.feeDecision.status[d.status]}</span>
        <span>{formatCents(d.totalFee)} €</span>
      </FixedSpaceColumn>
    )
  }
}

export const incomeRenderer: EventRenderer<TimelineIncome> = {
  color: () => '#99ff99',
  summary: (i: TimelineIncome) => {
    const { i18n } = useTranslation()
    return i18n.personProfile.income.details.effectOptions[i.effect]
  },
  tooltip: (i: TimelineIncome) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{i.range.format()}</span>
        <span>{i18n.personProfile.income.details.effectOptions[i.effect]}</span>
      </FixedSpaceColumn>
    )
  }
}

export const partnerRenderer: EventRenderer<TimelinePartnerDetailed> = {
  color: () => '#eb69ff',
  summary: (elem) => `Puoliso ${elem.firstName} ${elem.lastName}`,
  linkProvider: (elem) => `/profile/${elem.partnerId}`,
  tooltip: (p: TimelinePartnerDetailed) => (
    <FixedSpaceColumn spacing="xxs">
      <span>{p.range.format()}</span>
      <span>
        {p.firstName} {p.lastName}
      </span>
    </FixedSpaceColumn>
  ),
  nestedContent: (
    p: TimelinePartnerDetailed,
    timelineRange: FiniteDateRange,
    zoom: number
  ) => {
    const nestedRange = getNestedRange(p.range, timelineRange)
    if (nestedRange === null) return null

    return (
      <TlNestedContainer>
        {/*Fee decisions grouped by statuses*/}
        <TimelineGroup
          data={p.feeDecisions.filter((d) => d.status === 'SENT')}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={p.feeDecisions.filter((d) =>
            ['WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING'].includes(
              d.status
            )
          )}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={p.feeDecisions.filter((d) => d.status === 'DRAFT')}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={p.feeDecisions.filter((d) => d.status === 'ANNULLED')}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={p.incomes}
          renderer={incomeRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={p.children}
          renderer={childRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
      </TlNestedContainer>
    )
  }
}

export const childRenderer: EventRenderer<TimelineChildDetailed> = {
  color: () => '#ffff99',
  summary: (elem) => `Lapsi ${elem.firstName} ${elem.lastName}`,
  linkProvider: (elem) => `/child-information/${elem.childId}`,
  tooltip: (p: TimelineChildDetailed) => (
    <FixedSpaceColumn spacing="xxs">
      <span>{p.range.format()}</span>
      <span>
        {p.firstName} {p.lastName}
      </span>
      <span>s. {p.dateOfBirth.format()}</span>
    </FixedSpaceColumn>
  ),
  nestedContent: (
    p: TimelineChildDetailed,
    timelineRange: FiniteDateRange,
    zoom: number
  ) => {
    const nestedRange = getNestedRange(p.range, timelineRange)
    if (nestedRange === null) return null

    return (
      <TlNestedContainer>
        <TimelineGroup
          data={p.incomes}
          renderer={incomeRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={p.placements}
          renderer={placementRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={p.serviceNeeds}
          renderer={serviceNeedRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />
      </TlNestedContainer>
    )
  }
}

const getNestedRange = (range: DateRange, parentRange: FiniteDateRange) => {
  const minDate = maxOf(range.start, parentRange.start)
  const maxDate = range.end
    ? minOf(range.end, parentRange.end)
    : parentRange.end
  if (maxDate.isBefore(minDate)) {
    console.warn('Issue calculating nested range', range, parentRange)
    return null
  }
  return new FiniteDateRange(minDate, maxDate)
}

export const placementRenderer: EventRenderer<TimelinePlacement> = {
  color: () => '#e78b8b',
  summary: (p: TimelinePlacement) => {
    const { i18n } = useTranslation()
    return `${i18n.placement.type[p.type]} - ${p.unit.name}`
  },
  tooltip: (p: TimelinePlacement) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{p.range.format()}</span>
        <span>{i18n.placement.type[p.type]}</span>
        <span>{p.unit.name}</span>
      </FixedSpaceColumn>
    )
  }
}

export const serviceNeedRenderer: EventRenderer<TimelineServiceNeed> = {
  color: () => '#31a88f',
  summary: (sn: TimelineServiceNeed) => sn.name,
  tooltip: (sn: TimelineServiceNeed) => (
    <FixedSpaceColumn spacing="xxs">
      <span>{sn.range.format()}</span>
      <span>{sn.name}</span>
    </FixedSpaceColumn>
  )
}
