// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildPlacement,
  PlacementTerminationConstraint,
  PlacementTerminationRequestBody
} from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import { uniqBy } from 'lodash'
import React, { useCallback, useMemo, useState } from 'react'
import { useLang, useTranslation } from '../localization'
import { terminatePlacement } from './api'

interface UiState {
  placements: ChildPlacement[]
  terminationDate: string | undefined
}

const emptyState = (): UiState => ({
  placements: [],
  terminationDate: undefined
})

type TerminationFormState =
  | { type: 'valid'; data: PlacementTerminationRequestBody }
  | { type: 'invalid-date' }
  | { type: 'invalid-missing' }
  | { type: 'invalid-constraints' }

const validateTerminationDate = (
  terminationDate: LocalDate,
  placements: ChildPlacement[]
): boolean =>
  terminationDate.isEqualOrAfter(LocalDate.today()) &&
  placements.every((p) => p.placementEndDate.isAfter(terminationDate))

const checkTerminationConstraints = (
  constraints: PlacementTerminationConstraint[],
  placementIds: UUID[]
): boolean =>
  constraints.every(
    ({ placementId, requiresTerminationOf }) =>
      !placementIds.includes(placementId) ||
      placementIds.includes(requiresTerminationOf)
  )

interface Props {
  placements: ChildPlacement[]
  constraints: PlacementTerminationConstraint[]
  onSuccess: () => void
}

export default React.memo(function PlacementTerminationForm({
  constraints,
  placements,
  onSuccess
}: Props) {
  const t = useTranslation()
  const [lang] = useLang()

  const getPlacementLabel = (p: ChildPlacement) =>
    [
      t.placement.type[p.placementType],
      p.placementUnitName,
      t.children.placementTermination.until(p.placementEndDate.format())
    ].join(', ')

  const [state, setState] = useState<UiState>(emptyState())

  const isValidDate = useCallback(
    (date: LocalDate): boolean =>
      validateTerminationDate(date, state.placements),
    [state.placements]
  )

  const terminationState = useMemo<TerminationFormState>(() => {
    if (!(state.placements.length > 0 && state.terminationDate)) {
      return { type: 'invalid-missing' }
    }
    const date = LocalDate.parseFiOrNull(state.terminationDate)
    if (!(date && validateTerminationDate(date, state.placements))) {
      return { type: 'invalid-date' }
    }

    const placementIds = state.placements.map((p) => p.placementId)
    if (!checkTerminationConstraints(constraints, placementIds)) {
      return { type: 'invalid-constraints' }
    }

    return {
      type: 'valid',
      data: {
        placementIds: placementIds,
        terminationDate: date
      }
    }
  }, [constraints, state.placements, state.terminationDate])

  const onSubmit = useCallback(
    () =>
      terminationState.type === 'valid'
        ? terminatePlacement(terminationState.data)
        : Promise.reject('Invalid params'),
    [terminationState]
  )

  const onTerminateSuccess = useCallback(() => {
    setState(emptyState())
    onSuccess()
  }, [onSuccess])

  const togglePlacement = (p: ChildPlacement, checked: boolean) => {
    if (checked) {
      const otherPlacementsToTerminate = placements.filter((p2) =>
        constraints.some(
          (c) =>
            c.requiresTerminationOf === p2.placementId &&
            c.placementId === p.placementId
        )
      )
      setState((prev) => ({
        ...prev,
        placements: uniqBy(
          [...prev.placements, p, ...otherPlacementsToTerminate],
          ({ placementId }) => placementId
        )
      }))
    } else {
      const otherPlacementsToUncheck = constraints
        .filter((c) => c.requiresTerminationOf === p.placementId)
        .map(({ placementId }) => placementId)
      const placementsToUncheck = [p.placementId, ...otherPlacementsToUncheck]
      setState((prev) => ({
        ...prev,
        placements: prev.placements.filter(
          (p2) => !placementsToUncheck.includes(p2.placementId)
        )
      }))
    }
  }

  return (
    <>
      <div>
        <Label>{t.children.placementTermination.choosePlacement}</Label>
        {placements.map((p) => (
          <Checkbox
            key={p.placementId}
            label={getPlacementLabel(p)}
            checked={
              !!state.placements.find(
                ({ placementId }) => placementId === p.placementId
              )
            }
            onChange={(checked) => togglePlacement(p, checked)}
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
            terminationState.type === 'invalid-date'
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
        disabled={terminationState.type !== 'valid'}
        onClick={onSubmit}
        onSuccess={onTerminateSuccess}
      />
    </>
  )
})
