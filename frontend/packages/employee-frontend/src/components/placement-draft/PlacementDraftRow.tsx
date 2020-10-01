// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { faExclamationTriangle } from '@evaka/icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { areIntervalsOverlapping } from 'date-fns'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Title } from '~components/shared/alpha'

import { useTranslation } from '~state/i18n'
import { DatePicker } from '~components/common/DatePicker'
import {
  Period,
  DaycarePlacementPlan,
  PlacementType,
  PlacementDraft,
  PlacementDraftPlacement
} from '~types/placementdraft'
import { EspooColours } from '~utils/colours'

const DateRow = styled.div`
  display: flex;
  justify-content: flex-start;
  margin-bottom: 1.5rem;
`

interface DateRowItemProps {
  width?: string
  strong?: boolean
}

const DateRowItem = styled.span`
  display: flex;
  margin-top: auto;
  margin-bottom: auto;
  width: ${(props: DateRowItemProps) => props.width};
  font-weight: ${(props: DateRowItemProps) =>
    props.strong ? '600' : 'normal'};

  .react-datepicker-wrapper > div > div > input {
    width: 150px;
  }
`

const DateRowSpacer = styled.span`
  margin-left: 15px;
  margin-right: 15px;
  margin-top: auto;
  margin-bottom: auto;
`

const Container = styled.section`
  margin-bottom: 45px;
`

const OverlapError = styled.span`
  font-size: 12px;
  font-style: italic;
  font-weight: normal;
  color: ${EspooColours.greyDark};
  margin-bottom: auto;
  margin-top: auto;

  svg {
    font-size: 1rem;
    color: ${EspooColours.orange};
    margin-left: 10px;
    margin-right: 10px;
  }
`

const DateTitle = styled.div`
  margin-bottom: 30px;
`

interface Props {
  placementDraft: PlacementDraft
  placement: DaycarePlacementPlan
  updateStart: (date: LocalDate) => void
  updateEnd: (date: LocalDate) => void
  updatePreschoolStart: (date: LocalDate) => void
  updatePreschoolEnd: (date: LocalDate) => void
}

function PlacementDraftSection({
  placementDraft,
  placement,
  updateStart,
  updateEnd,
  updatePreschoolStart,
  updatePreschoolEnd
}: Props) {
  const { i18n } = useTranslation()

  function hasOverlap(
    period: Period,
    oldPlacements: PlacementDraftPlacement[]
  ) {
    return !!oldPlacements.find((placement: PlacementDraftPlacement) => {
      return areIntervalsOverlapping(
        {
          start: placement.startDate.toSystemTzDate(),
          end: placement.endDate.toSystemTzDate()
        },
        {
          start: period.start.toSystemTzDate(),
          end: period.end.toSystemTzDate()
        }
      )
    })
  }

  function dateRowLabel(type: PlacementType) {
    switch (type) {
      case 'CLUB':
        return i18n.common.types.CLUB
      case 'PRESCHOOL':
      case 'PRESCHOOL_DAYCARE':
        return i18n.common.types.PRESCHOOL
      case 'DAYCARE':
      case 'DAYCARE_PART_TIME':
        return i18n.common.types.DAYCARE
      case 'PREPARATORY':
      case 'PREPARATORY_DAYCARE':
        return i18n.common.types.PREPARATORY_EDUCATION
      default:
        return ''
    }
  }

  if (!placement.period) return null

  return (
    <Container>
      <DateTitle>
        <Title tag={3} size={4}>
          {i18n.placementDraft.datesTitle}
        </Title>
      </DateTitle>
      <DateRow>
        <DateRowItem width="225px" strong={true}>
          {i18n.placementDraft.type}
        </DateRowItem>
        <DateRowItem strong={true}>{i18n.placementDraft.date}</DateRowItem>
      </DateRow>
      <DateRow>
        <DateRowItem width="225px">
          {dateRowLabel(placementDraft.type)}
        </DateRowItem>
        <DateRowItem>
          <DatePicker
            date={placement.period.start}
            type="full-width"
            onChange={updateStart}
          />
          <DateRowSpacer>-</DateRowSpacer>
          <DatePicker
            date={placement.period.end}
            type="full-width"
            onChange={updateEnd}
          />
        </DateRowItem>
        <DateRowItem>
          {hasOverlap(placement.period, placementDraft.placements) && (
            <OverlapError>
              <FontAwesomeIcon icon={faExclamationTriangle} />
              {i18n.placementDraft.dateError}
            </OverlapError>
          )}
        </DateRowItem>
      </DateRow>
      {placement.preschoolDaycarePeriod && (
        <DateRow>
          <DateRowItem width="225px">
            {i18n.placementDraft.preschoolDaycare}
          </DateRowItem>
          <DateRowItem>
            <DatePicker
              date={placement.preschoolDaycarePeriod.start}
              type="full-width"
              onChange={updatePreschoolStart}
            />
            <DateRowSpacer>-</DateRowSpacer>
            <DatePicker
              date={placement.preschoolDaycarePeriod.end}
              type="full-width"
              onChange={updatePreschoolEnd}
            />
          </DateRowItem>
          {hasOverlap(
            placement.preschoolDaycarePeriod,
            placementDraft.placements
          ) && (
            <OverlapError>
              <FontAwesomeIcon icon={faExclamationTriangle} />
              {i18n.placementDraft.dateError}
            </OverlapError>
          )}
        </DateRow>
      )}
    </Container>
  )
}

export default PlacementDraftSection
