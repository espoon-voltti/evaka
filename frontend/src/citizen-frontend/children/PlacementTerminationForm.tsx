// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import { ChildPlacement } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import { useLang, useTranslation } from '../localization'
import { terminatePlacement, TerminatePlacementParams } from './api'

interface TerminationState {
  placement: ChildPlacement | undefined
  terminationDate: string | undefined
}

const emptyState = (): TerminationState => ({
  placement: undefined,
  terminationDate: undefined
})

interface Props {
  placements: ChildPlacement[]
  onSuccess: () => void
}

export default React.memo(function PlacementTerminationForm({
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

  const onTerminateSuccess = useCallback(() => {
    setState(emptyState())
    onSuccess()
  }, [onSuccess])

  return (
    <>
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
        onSuccess={onTerminateSuccess}
      />
    </>
  )
})
