// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
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
    props.strong ? fontWeights.semibold : fontWeights.normal};
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
  updatePeriod: (date: FiniteDateRange) => void
  updatePreschoolPeriod: (date: FiniteDateRange) => void
}

export default React.memo(function PlacementDraftSection({
  placementDraft,
  placement,
  updatePeriod,
  updatePreschoolPeriod
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
        <DateRowItem width="225px" id={`label-${placementDraft.type}`}>
          {dateRowLabel(placementDraft.type)}
        </DateRowItem>
        <DateRowItem>
          <DateRangePicker
            default={placement.period}
            onChange={(period) => period && updatePeriod(period)}
            locale="fi"
            errorTexts={i18n.validationErrors}
            labels={i18n.common.datePicker}
            aria-labelledby={`label-${placementDraft.type}`}
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
          <DateRowItem width="225px" id="preschool-placement-draft">
            {i18n.placementDraft.preschoolDaycare}
          </DateRowItem>
          <DateRowItem>
            <DateRangePicker
              default={placement.period}
              onChange={(period) => period && updatePreschoolPeriod(period)}
              locale="fi"
              errorTexts={i18n.validationErrors}
              labels={i18n.common.datePicker}
              aria-labelledby="preschool-placement-draft"
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
