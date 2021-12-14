// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  PlacementTerminationRequestBody,
  TerminatablePlacementGroup
} from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import React, { useCallback, useMemo, useState } from 'react'
import { useLang, useTranslation } from '../localization'
import { terminatePlacement } from './api'

type TerminationFormState =
  | { type: 'valid'; data: PlacementTerminationRequestBody }
  | { type: 'invalid-date' }
  | { type: 'invalid-missing' }

interface UiState {
  placement:
    | (TerminatablePlacementGroup & { terminateDayCareOnly?: boolean })
    | undefined
  terminationDate: string | undefined
}

const emptyState = (): UiState => ({
  placement: undefined,
  terminationDate: undefined
})

const validateTerminationDate = (
  terminationDate: LocalDate,
  group: TerminatablePlacementGroup | undefined
): boolean =>
  terminationDate.isEqualOrAfter(LocalDate.today()) &&
  (!group ||
    (terminationDate.isAfter(group.startDate) &&
      terminationDate.isBefore(group.endDate)))

interface Props {
  childId: UUID
  placementGroups: TerminatablePlacementGroup[]
  onSuccess: () => void
}

export default React.memo(function PlacementTerminationForm({
  childId,
  placementGroups,
  onSuccess
}: Props) {
  const t = useTranslation()
  const [lang] = useLang()

  const getPlacementLabel = (p: TerminatablePlacementGroup) =>
    [
      t.placement.type[p.type],
      p.unitName,
      t.children.placementTermination.until(p.endDate.format())
    ].join(', ')

  const [state, setState] = useState<UiState>(emptyState())

  const isValidDate = useCallback(
    (date: LocalDate): boolean =>
      validateTerminationDate(date, state.placement),
    [state.placement]
  )

  const terminationState = useMemo<TerminationFormState>(() => {
    if (!(state.placement && state.terminationDate)) {
      return { type: 'invalid-missing' }
    }
    const date = LocalDate.parseFiOrNull(state.terminationDate)
    if (!(date && validateTerminationDate(date, state.placement))) {
      return { type: 'invalid-date' }
    }

    return {
      type: 'valid',
      data: {
        type: state.placement.type,
        unitId: state.placement.unitId,
        terminateDaycareOnly: state.placement.terminateDayCareOnly ?? false,
        terminationDate: date
      }
    }
  }, [state.placement, state.terminationDate])

  const onSubmit = useCallback(
    () =>
      terminationState.type === 'valid'
        ? terminatePlacement(childId, terminationState.data)
        : Promise.reject('Invalid params'),
    [childId, terminationState]
  )

  const onTerminateSuccess = useCallback(() => {
    setState(emptyState())
    onSuccess()
  }, [onSuccess])

  const togglePlacement = (p: TerminatablePlacementGroup, checked: boolean) => {
    setState((prev) => ({ ...prev, placement: checked ? p : undefined }))
  }

  return (
    <>
      <div>
        <Label>{t.children.placementTermination.choosePlacement}</Label>
        {placementGroups.map((p) => (
          <Checkbox
            key={`${p.type} ${p.unitId}`}
            label={getPlacementLabel(p)}
            checked={
              state.placement?.unitId === p.unitId &&
              state.placement.type === p.type
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
