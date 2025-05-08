// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  PlacementPlanDraft,
  PlacementSummary,
  PlacementType
} from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DateRangePicker'
import { fontWeights, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import { DaycarePlacementPlanForm } from './PlacementDraft'

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
  placementDraft: PlacementPlanDraft
  formState: DaycarePlacementPlanForm
  updateStart: (date: LocalDate | null) => void
  updateEnd: (date: LocalDate | null) => void
  updatePreschoolStart: (date: LocalDate | null) => void
  updatePreschoolEnd: (date: LocalDate | null) => void
}

export default React.memo(function PlacementDraftSection({
  placementDraft,
  formState,
  updateStart,
  updateEnd,
  updatePreschoolStart,
  updatePreschoolEnd
}: Props) {
  const { i18n } = useTranslation()

  const today = LocalDate.todayInSystemTz()

  function hasOverlap(
    dateRange: FiniteDateRange,
    oldPlacements: PlacementSummary[]
  ) {
    return !!oldPlacements.find((placement: PlacementSummary) =>
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
          <DatePicker
            date={formState.period?.start ?? null}
            onChange={updateStart}
            minDate={today}
            locale="fi"
            data-qa="start-date"
          />
          <DatePickerSpacer />
          <DatePicker
            date={formState.period?.end ?? null}
            onChange={updateEnd}
            minDate={formState.period?.start ?? today}
            locale="fi"
          />
        </DateRowItem>
        <DateRowItem>
          {formState.period &&
            hasOverlap(formState.period, placementDraft.placements) && (
              <OverlapError>
                <FontAwesomeIcon icon={faExclamationTriangle} />
                {i18n.placementDraft.dateError}
              </OverlapError>
            )}
        </DateRowItem>
      </DateRow>
      {formState.hasPreschoolDaycarePeriod && (
        <DateRow>
          <DateRowItem width="225px">
            {i18n.placementDraft.preschoolDaycare}
          </DateRowItem>
          <DateRowItem>
            <DatePicker
              date={formState.preschoolDaycarePeriod?.start ?? null}
              onChange={updatePreschoolStart}
              minDate={today}
              locale="fi"
            />
            <DatePickerSpacer />
            <DatePicker
              date={formState.preschoolDaycarePeriod?.end ?? null}
              onChange={updatePreschoolEnd}
              minDate={formState.preschoolDaycarePeriod?.start ?? today}
              locale="fi"
            />
          </DateRowItem>
          {formState.preschoolDaycarePeriod !== null &&
            hasOverlap(
              formState.preschoolDaycarePeriod,
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
