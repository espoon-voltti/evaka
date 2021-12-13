// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildPlacement } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label, P } from 'lib-components/typography'
import React, { useCallback, useMemo, useState } from 'react'
import { renderResult } from '../async-rendering'
import { useLang, useTranslation } from '../localization'
import {
  getPlacements,
  terminatePlacement,
  TerminatePlacementParams
} from './api'

interface TerminationState {
  placement: ChildPlacement | undefined
  terminationDate: string | undefined
}
const emptyState = (): TerminationState => ({
  placement: undefined,
  terminationDate: undefined
})

interface PlacementTerminationProps {
  childId: UUID
}

export default React.memo(function PlacementTerminationSection({
  childId
}: PlacementTerminationProps) {
  const [lang] = useLang()
  const t = useTranslation()
  const [placementsResponse, refreshPlacements] = useApiState(
    () => getPlacements(childId),
    [childId]
  )

  const getPlacementLabel = (p: ChildPlacement) =>
    [
      t.placement.type[p.placementType],
      p.placementUnitName,
      t.children.placementTermination.until(p.placementEndDate.format())
    ].join(', ')

  const [state, setState] = useState<TerminationState>(emptyState())

  const isValidDate = useCallback(
    (date: LocalDate): boolean =>
      date.isEqualOrAfter(LocalDate.today()) &&
      (!state.placement || state.placement.placementEndDate.isAfter(date)),
    [state.placement]
  )

  const terminatePlacementParams = useMemo<
    TerminatePlacementParams | 'invalid-date' | 'missing'
  >(() => {
    if (!(state.placement && state.terminationDate)) {
      return 'missing'
    }
    const date = LocalDate.parseFiOrNull(state.terminationDate)
    return date && isValidDate(date)
      ? {
          id: state.placement.placementId,
          terminationDate: date
        }
      : 'invalid-date'
  }, [isValidDate, state.placement, state.terminationDate])

  const isValid = typeof terminatePlacementParams !== 'string'
  const onSubmit = useCallback(
    () =>
      typeof terminatePlacementParams !== 'string'
        ? terminatePlacement(terminatePlacementParams)
        : Promise.reject('Invalid params'),
    [terminatePlacementParams]
  )

  const onSuccess = useCallback(() => {
    setState(emptyState())
    refreshPlacements()
  }, [refreshPlacements])

  return (
    <CollapsibleSection
      title={t.children.placementTermination.title}
      startCollapsed
      fitted
    >
      {renderResult(placementsResponse, (placements) => (
        <FixedSpaceColumn>
          <P>{t.children.placementTermination.description}</P>
          <div>
            <Label>{t.children.placementTermination.choosePlacement}</Label>
            {placements.map((p) => (
              <Checkbox
                key={p.placementId}
                label={getPlacementLabel(p)}
                checked={state.placement?.placementId === p.placementId}
                onChange={(checked) =>
                  setState((prev) => ({
                    ...prev,
                    placement: checked ? p : undefined
                  }))
                }
              />
            ))}
          </div>
          <div>
            <ExpandingInfo
              info={t.children.placementTermination.lastDayInfo}
              ariaLabel={t.common.openExpandingInfo}
            >
              <Label>{t.children.placementTermination.lastDayOfPresence}</Label>
            </ExpandingInfo>
            <DatePicker
              hideErrorsBeforeTouched
              required
              locale={lang}
              isValidDate={isValidDate}
              info={
                terminatePlacementParams === 'invalid-date'
                  ? { text: t.validationErrors.timeFormat, status: 'warning' }
                  : undefined
              }
              date={state.terminationDate ?? ''}
              onChange={(terminationDate) =>
                setState((prev) => ({ ...prev, terminationDate }))
              }
              openAbove
            />
          </div>

          <AsyncButton
            primary
            text={t.children.placementTermination.terminate}
            disabled={!isValid}
            onClick={onSubmit}
            onSuccess={onSuccess}
          />
        </FixedSpaceColumn>
      ))}
    </CollapsibleSection>
  )
})
