// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import { BoundForm, BoundFormState, useFormElem } from 'lib-common/form/hooks'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { faPlus, fasUserMinus, faTrash, faUserMinus } from 'lib-icons'

import TimeRangeInput from '../TimeRangeInput'

import { emptyTimeRange, times } from './form'

interface TimeInputsProps {
  bindPresent?: BoundFormState<boolean> | undefined
  bindTimes: BoundForm<typeof times>
  mode: 'normal' | 'not-editable' | 'holiday' | undefined
  label: JSX.Element
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

export default React.memo(function TimeInputs({
  mode,
  bindPresent,
  bindTimes,
  label,
  showAllErrors,
  allowExtraTimeRange,
  dataQaPrefix,
  onFocus
}: TimeInputsProps) {
  const i18n = useTranslation()
  const [infoOpen, setInfoOpen] = useState(false)
  const onInfoClick = useCallback(() => setInfoOpen((prev) => !prev), [])

  const firstTimeRange = useFormElem(bindTimes, 0)
  const secondTimeRange = useFormElem(bindTimes, 1)

  if (firstTimeRange === undefined) {
    throw new Error('BUG: at least one time range expected')
  }

  if (mode === undefined) {
    return (
      <FixedSpaceRow fullWidth>
        <LeftCell>{label}</LeftCell>
        <MiddleCell />
        <RightCell />
      </FixedSpaceRow>
    )
  }
  if (mode === 'not-editable') {
    return (
      <>
        <FixedSpaceRow fullWidth>
          <LeftCell>{label}</LeftCell>
          <MiddleCell textOnly>
            {i18n.calendar.reservationModal.absent}
          </MiddleCell>
          <RightCell>
            <InfoButton
              onClick={onInfoClick}
              aria-label={i18n.common.openExpandingInfo}
              data-qa="not-editable-info-button"
            />
          </RightCell>
        </FixedSpaceRow>
        {infoOpen && (
          <FixedSpaceRow fullWidth>
            <ExpandingInfoBox
              data-qa={
                dataQaPrefix
                  ? `${dataQaPrefix}-absence-by-employee-info-box`
                  : undefined
              }
              close={onInfoClick}
              aria-label={i18n.calendar.absenceMarkedByEmployee}
              info={i18n.calendar.contactStaffToEditAbsence}
              closeLabel={i18n.common.close}
              width="full"
            />
          </FixedSpaceRow>
        )}
      </>
    )
  }

  if (mode === 'holiday') {
    return (
      <FixedSpaceRow fullWidth>
        <LeftCell>{label}</LeftCell>
        <MiddleCell textOnly>{i18n.calendar.holiday}</MiddleCell>
        <RightCell />
      </FixedSpaceRow>
    )
  }

  if (bindPresent && !bindPresent.value()) {
    return (
      <FixedSpaceRow fullWidth>
        <LeftCell>{label}</LeftCell>
        <MiddleCell textOnly>
          {i18n.calendar.reservationModal.absent}
        </MiddleCell>
        <RightCell>
          <IconButton
            data-qa={dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined}
            icon={fasUserMinus}
            onClick={() => {
              bindPresent.set(true)
              bindTimes.set([emptyTimeRange])
            }}
            aria-label={i18n.calendar.absentDisable}
          />
        </RightCell>
      </FixedSpaceRow>
    )
  }

  return (
    <FixedSpaceRow fullWidth>
      <LeftCell>{label}</LeftCell>
      <MiddleCell>
        <TimeRangeInput
          bind={firstTimeRange}
          hideErrorsBeforeTouched={!showAllErrors}
          data-qa={dataQaPrefix ? `${dataQaPrefix}-time-0` : undefined}
          onFocus={onFocus}
        />
      </MiddleCell>
      <RightCell>
        <FixedSpaceRow>
          {bindPresent !== undefined ? (
            <IconButton
              data-qa={
                dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined
              }
              icon={faUserMinus}
              onClick={() => bindPresent.set(false)}
              aria-label={i18n.calendar.absentEnable}
            />
          ) : null}
          {secondTimeRange === undefined && allowExtraTimeRange ? (
            <IconButton
              icon={faPlus}
              onClick={() =>
                bindTimes.update((prev) => [prev[0], emptyTimeRange])
              }
              aria-label={i18n.common.add}
            />
          ) : (
            <div />
          )}
        </FixedSpaceRow>
      </RightCell>
      {secondTimeRange !== undefined ? (
        <FixedSpaceRow fullWidth>
          <LeftCell />
          <MiddleCell>
            <TimeRangeInput
              bind={secondTimeRange}
              hideErrorsBeforeTouched={!showAllErrors}
              data-qa={dataQaPrefix ? `${dataQaPrefix}-time-1` : undefined}
              onFocus={onFocus}
            />
          </MiddleCell>
          <RightCell>
            <IconButton
              icon={faTrash}
              onClick={() => bindTimes.update((prev) => prev.slice(0, 1))}
              aria-label={i18n.common.delete}
            />
          </RightCell>
        </FixedSpaceRow>
      ) : null}
    </FixedSpaceRow>
  )
})

const LeftCell = styled.div`
  flex: 0.25;
  padding-top: 8px;
`
const MiddleCell = styled.div<{ textOnly?: boolean }>`
  flex: 0.7;
  ${(p) => p.textOnly && 'padding-top: 8px;'}
`
const RightCell = styled.div`
  flex: 0.15;
  padding-top: 8px;
  align-self: center;
`
