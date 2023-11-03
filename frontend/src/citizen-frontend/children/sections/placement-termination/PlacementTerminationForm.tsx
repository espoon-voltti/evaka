// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import first from 'lodash/first'
import last from 'lodash/last'
import maxBy from 'lodash/maxBy'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'

import {
  PlacementTerminationRequestBody,
  PlacementType,
  TerminatablePlacementGroup
} from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H3, Label } from 'lib-components/typography'

import ModalAccessibilityWrapper from '../../../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../../../localization'

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
  terminationDate: LocalDate | null
}

const emptyState = (): UiState => ({
  placements: [],
  terminationDate: null
})

const toCheckboxOption = (
  grp: TerminatablePlacementGroup,
  terminateDayCareOnly = false
): CheckboxOption => ({
  ...grp,
  terminateDaycareOnly: terminateDayCareOnly,
  pseudoId: `${grp.unitId}-${grp.type}-${String(terminateDayCareOnly)}`
})

const maybeCreateDaycareOnlyTerminatable = (
  grp: TerminatablePlacementGroup
): CheckboxOption[] => {
  const isPreschoolDaycareOrPreparatoryDaycare = (p: { type: PlacementType }) =>
    p.type === 'PRESCHOOL_DAYCARE' ||
    p.type === 'PRESCHOOL_CLUB' ||
    p.type === 'PREPARATORY_DAYCARE'

  const firstBilledStartDate =
    grp.placements.find(isPreschoolDaycareOrPreparatoryDaycare)?.startDate ??
    first(grp.additionalPlacements)?.startDate

  if (!firstBilledStartDate) {
    return []
  }

  const lastBilledEndDate =
    last(
      sortBy(
        [
          ...grp.placements.filter(isPreschoolDaycareOrPreparatoryDaycare),
          ...grp.additionalPlacements
        ],
        (a) => a.startDate
      )
    )?.endDate ?? grp.endDate
  return [
    {
      ...toCheckboxOption(grp, true),
      startDate: firstBilledStartDate,
      endDate: lastBilledEndDate
    }
  ]
}

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

  const getPlacementLabel = useCallback(
    (p: CheckboxOption) =>
      [
        p.terminateDaycareOnly
          ? t.children.placementTermination.invoicedDaycare
          : t.placement.type[p.type],
        p.unitName,
        t.children.placementTermination.until(p.endDate.format())
      ].join(', '),
    [t]
  )

  const [showConfirmDialog, setShowConfirmDialog] = useState(false)
  const [state, setState] = useState<UiState>(emptyState())

  const terminationState = useMemo<TerminationFormState>(() => {
    if (!(state.placements.length > 0 && state.terminationDate)) {
      return { type: 'invalid-missing' }
    }
    if (!state.terminationDate) {
      return { type: 'invalid-date' }
    }

    const terminateDaycareOnly = state.placements.every(
      (p) => p.terminateDaycareOnly
    )
    return {
      type: 'valid',
      data: {
        type: state.placements[0].type,
        unitId: state.placements[0].unitId,
        terminateDaycareOnly,
        terminationDate: state.terminationDate
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
    setShowConfirmDialog(false)
    setState(emptyState())
    onSuccess()
  }, [onSuccess])

  // Add option for terminating only the invoiced placements if in preschool or preparatory placement
  const options: CheckboxOption[] = useMemo(
    () => [
      toCheckboxOption(placementGroup),
      ...maybeCreateDaycareOnlyTerminatable(placementGroup)
    ],
    [placementGroup]
  )

  const onTogglePlacement = (opt: CheckboxOption) => (checked: boolean) => {
    if (checked) {
      setState((prev) => ({
        ...prev,
        placements: opt.terminateDaycareOnly ? [opt] : options
      }))
    } else {
      setState((prev) => ({
        ...prev,
        placements: opt.terminateDaycareOnly
          ? []
          : prev.placements.filter(({ pseudoId }) => pseudoId !== opt.pseudoId)
      }))
    }
  }

  return (
    <>
      <H3>{t.placement.type[placementGroup.type]}</H3>
      <div>
        <Label>{t.children.placementTermination.choosePlacement}</Label>
        {sortBy(options, (p) => p.startDate).map((p) => (
          <Checkbox
            data-qa="placement"
            key={p.pseudoId}
            label={getPlacementLabel(p)}
            checked={
              !!state.placements.find(({ pseudoId }) => pseudoId === p.pseudoId)
            }
            onChange={onTogglePlacement(p)}
          />
        ))}
      </div>
      <div>
        <ExpandingInfo
          info={t.children.placementTermination.lastDayInfo}
          inlineChildren
        >
          <Label>{t.children.placementTermination.lastDayOfPresence}</Label>
        </ExpandingInfo>
        <DatePicker
          data-qa="termination-date"
          hideErrorsBeforeTouched
          required
          locale={lang}
          minDate={LocalDate.todayInSystemTz()}
          maxDate={useMemo(
            () => maxBy(state.placements, (p) => p.endDate)?.endDate,
            [state.placements]
          )}
          info={
            terminationState.type === 'invalid-date'
              ? { text: t.validationErrors.timeFormat, status: 'warning' }
              : undefined
          }
          date={state.terminationDate}
          onChange={(terminationDate) =>
            setState((prev) => ({ ...prev, terminationDate }))
          }
          openAbove
        />
      </div>

      <Button
        primary
        text={t.children.placementTermination.terminate}
        disabled={terminationState.type !== 'valid'}
        onClick={() => setShowConfirmDialog(true)}
      />

      {showConfirmDialog && terminationState.type === 'valid' && (
        <ModalAccessibilityWrapper>
          <AsyncFormModal
            title={t.children.placementTermination.confirmQuestion}
            text={t.children.placementTermination.confirmDescription(
              terminationState.data.terminationDate.format()
            )}
            resolveAction={onSubmit}
            resolveLabel={t.children.placementTermination.terminate}
            onSuccess={onTerminateSuccess}
            rejectAction={() => setShowConfirmDialog(false)}
            rejectLabel={t.common.cancel}
          />
        </ModalAccessibilityWrapper>
      )}
    </>
  )
})
