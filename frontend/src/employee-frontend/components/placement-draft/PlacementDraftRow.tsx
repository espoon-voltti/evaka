// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { fontWeights, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import {
  DaycarePlacementPlan,
  PlacementDraft,
  PlacementDraftPlacement
} from '../../types/placementdraft'

const DateRow = styled.div`
  display: flex;
  justify-content: flex-start;
  margin-bottom: 1.5rem;
`

const DateRowItem = styled.span<{
  width?: string
  strong?: boolean
}>`
  display: flex;
  margin-top: auto;
  margin-bottom: auto;
  width: ${(p) => p.width};
  font-weight: ${(p) => (p.strong ? fontWeights.semibold : fontWeights.normal)};

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

const OverlapError = styled.span`
  font-size: 12px;
  font-style: italic;
  font-weight: ${fontWeights.normal};
  color: ${colors.grayscale.g70};
  margin-bottom: auto;
  margin-top: auto;

  svg {
    font-size: 1rem;
    color: ${colors.status.warning};
    margin-left: 10px;
    margin-right: 10px;
  }
`

interface Props {
  placementDraft: PlacementDraft
  placement: DaycarePlacementPlan
  updateStart: (date: LocalDate) => void
  updateEnd: (date: LocalDate) => void
  updatePreschoolStart: (date: LocalDate) => void
  updatePreschoolEnd: (date: LocalDate) => void
}

export default React.memo(function PlacementDraftSection({
  placementDraft,
  placement,
  updateStart,
  updateEnd,
  updatePreschoolStart,
  updatePreschoolEnd
}: Props) {
  const { i18n } = useTranslation()

  function hasOverlap(
    dateRange: FiniteDateRange,
    oldPlacements: PlacementDraftPlacement[]
  ) {
    return !!oldPlacements.find((placement: PlacementDraftPlacement) =>
      dateRange.overlaps(
        new FiniteDateRange(placement.startDate, placement.endDate)
      )
    )
  }

  function dateRowLabel(type: PlacementType) {
    switch (type) {
      case 'CLUB':
        return i18n.common.types.CLUB
      case 'PRESCHOOL':
      case 'PRESCHOOL_DAYCARE':
      case 'PRESCHOOL_CLUB':
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
    <section>
      <H2 noMargin>{i18n.placementDraft.datesTitle}</H2>
      <Gap size="s" />
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
          <DatePickerDeprecated
            date={placement.period.start}
            type="full-width"
            onChange={updateStart}
            minDate={LocalDate.todayInSystemTz()}
            data-qa="start-date"
          />
          <DateRowSpacer>-</DateRowSpacer>
          <DatePickerDeprecated
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
            <DatePickerDeprecated
              date={placement.preschoolDaycarePeriod.start}
              type="full-width"
              onChange={updatePreschoolStart}
            />
            <DateRowSpacer>-</DateRowSpacer>
            <DatePickerDeprecated
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
    </section>
  )
})
