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
import { H3, Label } from 'lib-components/typography'
import { sortBy } from 'lodash'
import React, { useCallback, useMemo, useState } from 'react'
import { useLang, useTranslation } from '../localization'
import { terminatePlacement } from './api'

type TerminationFormState =
  | { type: 'valid'; data: PlacementTerminationRequestBody }
  | { type: 'invalid-date' }
  | { type: 'invalid-missing' }

type CheckboxOption = TerminatablePlacementGroup & {
  pseudoId: string // unit+type+terminateDayCareOnly
  terminateDaycareOnly: boolean
}

interface UiState {
  placements: CheckboxOption[]
  terminationDate: string | undefined
}

const emptyState = (): UiState => ({
  placements: [],
  terminationDate: undefined
})

const validateTerminationDate = (
  terminationDate: LocalDate,
  options: CheckboxOption[]
): boolean =>
  terminationDate.isEqualOrAfter(LocalDate.today()) &&
  options.every((o) => terminationDate.isBefore(o.endDate))

const toCheckboxOption = (
  grp: TerminatablePlacementGroup,
  terminateDayCareOnly = false
): CheckboxOption => ({
  ...grp,
  terminateDaycareOnly: terminateDayCareOnly,
  pseudoId: `${grp.unitId}-${grp.type}-${String(terminateDayCareOnly)}`
})

const toDaycareOnlyTerminatable = (
  grp: TerminatablePlacementGroup
): CheckboxOption => ({
  ...toCheckboxOption(grp, true),
  startDate: grp.additionalPlacements[0].startDate,
  endDate: grp.additionalPlacements[grp.additionalPlacements.length - 1].endDate
})

interface Props {
  childId: UUID
  placementGroup: TerminatablePlacementGroup
  onSuccess: () => void
}

export default React.memo(function PlacementTerminationForm({
  childId,
  placementGroup,
  onSuccess
}: Props) {
  const t = useTranslation()
  const [lang] = useLang()

  const getPlacementLabel = (p: CheckboxOption) => {
    const type = p.terminateDaycareOnly
      ? t.children.placementTermination.invoicedDaycare
      : t.placement.type[p.type]
    return [
      type,
      p.unitName,
      p.startDate.format() + ' – ' + p.endDate.format()
    ].join(', ')
  }

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

    return {
      type: 'valid',
      data: {
        type: state.placements[0].type,
        unitId: state.placements[0].unitId,
        terminateDaycareOnly: state.placements[0].terminateDaycareOnly ?? false,
        terminationDate: date
      }
    }
  }, [state.placements, state.terminationDate])

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

  // Add option for terminating only the invoiced placements if in preschool or preparatory placement
  const options: CheckboxOption[] = useMemo(
    () =>
      placementGroup.additionalPlacements.length > 0
        ? [
            toCheckboxOption(placementGroup),
            toDaycareOnlyTerminatable(placementGroup)
          ]
        : [toCheckboxOption(placementGroup)],
    [placementGroup]
  )

  return (
    <>
      <div>
        <H3>{t.placement.type[placementGroup.type]}</H3>
        <Label>{t.children.placementTermination.choosePlacement}</Label>
        {sortBy(options, (p) => p.startDate).map((p) => (
          <Checkbox
            key={p.pseudoId}
            label={getPlacementLabel(p)}
            checked={
              !!state.placements.find((p2) => p2.pseudoId === p.pseudoId)
            }
            onChange={(checked) => {
              if (checked) {
                setState((prev) => ({
                  ...prev,
                  placements: p.terminateDaycareOnly ? [p] : options
                }))
              } else {
                setState((prev) => ({
                  ...prev,
                  placements: p.terminateDaycareOnly
                    ? []
                    : prev.placements.filter((p2) => p2.pseudoId !== p.pseudoId)
                }))
              }
            }}
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
