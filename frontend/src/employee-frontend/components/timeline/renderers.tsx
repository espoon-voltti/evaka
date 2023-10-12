// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  TimelineChildDetailed,
  TimelineFeeAlteration,
  TimelineFeeDecision,
  TimelineIncome,
  TimelinePartnerDetailed,
  TimelinePlacement,
  TimelineServiceNeed,
  TimelineValueDecision
} from 'lib-common/generated/api-types/timeline'
import { formatCents } from 'lib-common/money'
import { maxOf, minOf } from 'lib-common/ordered'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import TimelineGroup from './TimelineGroup'
import { WithRange } from './common'

type SummaryRenderer<T extends WithRange> = (props: {
  elem: T
}) => React.ReactNode
type TooltipRenderer<T extends WithRange> = (props: {
  elem: T
}) => React.ReactNode
type NestedContentRenderer<T extends WithRange> = (props: {
  elem: T
  timelineRange: FiniteDateRange
  zoom: number
}) => React.ReactNode
export interface EventRenderer<T extends WithRange> {
  color: (elem: T) => string
  linkProvider?: (elem: T) => string
  Summary: SummaryRenderer<T>
  Tooltip?: TooltipRenderer<T>
  NestedContent?: NestedContentRenderer<T>
}

const TlNestedContainer = styled.div`
  width: 100%;
  min-height: 30px;
  display: flex;
  flex-direction: column;
`

export const monthRenderer: EventRenderer<WithRange> = {
  color: () => '#ffffff',
  Summary: ({ elem }) => elem.range.start.format('MM/yyyy')
}

export const feeDecisionRenderer: EventRenderer<TimelineFeeDecision> = {
  color: ({ status }) => {
    switch (status) {
      case 'SENT':
      case 'WAITING_FOR_SENDING':
      case 'WAITING_FOR_MANUAL_SENDING':
        return '#80f6ff'
      case 'DRAFT':
        return '#c3e1e0'
      case 'ANNULLED':
        return '#aeb6b7'
      case 'IGNORED':
        return '#e8e8e8'
    }
  },
  linkProvider: (elem) => `/finance/fee-decisions/${elem.id}`,
  Summary: ({ elem }) => {
    const { i18n } = useTranslation()
    return `${i18n.timeline.feeDecision} ${
      i18n.feeDecision.status[elem.status]
    }`
  },
  Tooltip: ({ elem }) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{elem.range.format()}</span>
        <span>{i18n.feeDecision.status[elem.status]}</span>
        <span>{formatCents(elem.totalFee)} €</span>
      </FixedSpaceColumn>
    )
  }
}

export const valueDecisionRenderer: EventRenderer<TimelineValueDecision> = {
  color: ({ status }) => {
    switch (status) {
      case 'SENT':
      case 'WAITING_FOR_SENDING':
      case 'WAITING_FOR_MANUAL_SENDING':
        return '#80f6ff'
      case 'DRAFT':
        return '#c3e1e0'
      case 'ANNULLED':
        return '#aeb6b7'
      case 'IGNORED':
        return '#e8e8e8'
    }
  },
  linkProvider: (elem) => `/finance/value-decisions/${elem.id}`,
  Summary: ({ elem }) => {
    const { i18n } = useTranslation()
    return `${i18n.timeline.valueDecision} ${
      i18n.valueDecision.status[elem.status]
    }`
  },
  Tooltip: ({ elem }) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{elem.range.format()}</span>
        <span>{i18n.valueDecision.status[elem.status]}</span>
      </FixedSpaceColumn>
    )
  }
}

export const incomeRenderer: EventRenderer<TimelineIncome> = {
  color: () => '#99ff99',
  Summary: ({ elem }) => {
    const { i18n } = useTranslation()
    return i18n.personProfile.income.details.effectOptions[elem.effect]
  },
  Tooltip: ({ elem }) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{elem.range.format()}</span>
        <span>
          {i18n.personProfile.income.details.effectOptions[elem.effect]}
        </span>
      </FixedSpaceColumn>
    )
  }
}

export const partnerRenderer: EventRenderer<TimelinePartnerDetailed> = {
  color: () => '#f4bcff',
  linkProvider: (elem) => `/profile/${elem.partnerId}`,
  Summary: ({ elem }) => {
    const { i18n } = useTranslation()
    return `${i18n.timeline.partner} ${elem.firstName} ${elem.lastName}`
  },
  Tooltip: ({ elem }) => (
    <FixedSpaceColumn spacing="xxs">
      <span>{elem.range.format()}</span>
      <span>
        {elem.firstName} {elem.lastName}
      </span>
    </FixedSpaceColumn>
  ),
  NestedContent: ({ elem, timelineRange, zoom }) => {
    const nestedRange = getNestedRange(elem.range, timelineRange)
    if (nestedRange === null) return null

    return (
      <TlNestedContainer>
        {/*Fee decisions grouped by statuses*/}
        <TimelineGroup
          data={elem.feeDecisions.filter((d) => d.status === 'SENT')}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={elem.feeDecisions.filter((d) =>
            ['WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING'].includes(
              d.status
            )
          )}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={elem.feeDecisions.filter((d) => d.status === 'DRAFT')}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={elem.feeDecisions.filter((d) => d.status === 'ANNULLED')}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={elem.feeDecisions.filter((d) => d.status === 'IGNORED')}
          renderer={feeDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        {/*Value decisions grouped by statuses*/}
        <TimelineGroup
          data={elem.valueDecisions.filter((d) => d.status === 'SENT')}
          renderer={valueDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={elem.valueDecisions.filter((d) =>
            ['WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING'].includes(
              d.status
            )
          )}
          renderer={valueDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={elem.valueDecisions.filter((d) => d.status === 'DRAFT')}
          renderer={valueDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
        <TimelineGroup
          data={elem.valueDecisions.filter((d) => d.status === 'ANNULLED')}
          renderer={valueDecisionRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={elem.incomes}
          renderer={incomeRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={elem.children}
          renderer={childRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
      </TlNestedContainer>
    )
  }
}

export const childRenderer: EventRenderer<TimelineChildDetailed> = {
  color: () => '#ffffc1',
  linkProvider: (elem) => `/child-information/${elem.childId}`,
  Summary: ({ elem }) => {
    const { i18n } = useTranslation()
    return `${i18n.timeline.child} ${elem.firstName} ${elem.lastName}`
  },
  Tooltip: ({ elem }) => (
    <FixedSpaceColumn spacing="xxs">
      <span>{elem.range.format()}</span>
      <span>
        {elem.firstName} {elem.lastName}
      </span>
      <span>s. {elem.dateOfBirth.format()}</span>
    </FixedSpaceColumn>
  ),
  NestedContent: ({ elem, timelineRange, zoom }) => {
    const nestedRange = getNestedRange(elem.range, timelineRange)
    if (nestedRange === null) return null

    return (
      <TlNestedContainer>
        <TimelineGroup
          data={elem.placements}
          renderer={placementRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={elem.serviceNeeds}
          renderer={serviceNeedRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={elem.incomes}
          renderer={incomeRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />

        <Gap size="xs" />

        <TimelineGroup
          data={elem.feeAlterations}
          renderer={feeAlterationRenderer}
          timelineRange={nestedRange}
          zoom={zoom}
        />
      </TlNestedContainer>
    )
  }
}

export const placementRenderer: EventRenderer<TimelinePlacement> = {
  color: () => '#ffb4b4',
  Summary: ({ elem }) => {
    const { i18n } = useTranslation()
    return `${i18n.placement.type[elem.type]} - ${elem.unit.name}`
  },
  Tooltip: ({ elem }) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{elem.range.format()}</span>
        <span>{i18n.placement.type[elem.type]}</span>
        <span>{elem.unit.name}</span>
      </FixedSpaceColumn>
    )
  }
}

export const serviceNeedRenderer: EventRenderer<TimelineServiceNeed> = {
  color: () => '#5fdaa3',
  Summary: ({ elem }) => elem.name,
  Tooltip: ({ elem }) => (
    <FixedSpaceColumn spacing="xxs">
      <span>{elem.range.format()}</span>
      <span>{elem.name}</span>
    </FixedSpaceColumn>
  )
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

export const feeAlterationRenderer: EventRenderer<TimelineFeeAlteration> = {
  color: () => '#bbde80',
  Summary: ({ elem }) => {
    const { i18n } = useTranslation()
    return i18n.feeAlteration[elem.type]
  },
  Tooltip: ({ elem }) => {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn spacing="xxs">
        <span>{elem.range.format()}</span>
        <span>
          {i18n.feeAlteration[elem.type]} {elem.amount}{' '}
          {elem.absolute ? '€' : '%'}
        </span>
        <span>{elem.notes}</span>
      </FixedSpaceColumn>
    )
  }
}
