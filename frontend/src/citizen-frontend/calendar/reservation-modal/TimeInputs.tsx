// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import {
  BoundForm,
  BoundFormState,
  useFormElem,
  useFormFields,
  useFormUnion
} from 'lib-common/form/hooks'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { faPlus, fasUserMinus, faTrash, faUserMinus } from 'lib-icons'

import TimeRangeInput from '../TimeRangeInput'

import { day, emptyTimeRange, times, writableDay } from './form'

interface DayProps {
  bind: BoundForm<typeof day>
  label: React.ReactNode
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

export const Day = React.memo(function Day({
  bind,
  label,
  showAllErrors,
  allowExtraTimeRange,
  dataQaPrefix,
  onFocus
}: DayProps) {
  const { branch, form } = useFormUnion(bind)

  if (branch === 'readOnly') {
    return (
      <ReadOnlyDay
        mode={form.state}
        label={label}
        dataQaPrefix={dataQaPrefix}
      />
    )
  } else {
    return (
      <WritableDay
        bind={form}
        label={label}
        showAllErrors={showAllErrors}
        allowExtraTimeRange={allowExtraTimeRange}
        dataQaPrefix={dataQaPrefix}
        onFocus={onFocus}
      />
    )
  }
})

interface WritableDayProps {
  bind: BoundForm<typeof writableDay>
  label: React.ReactNode
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

const WritableDay = React.memo(function WritableDay({
  bind,
  label,
  showAllErrors,
  allowExtraTimeRange,
  dataQaPrefix,
  onFocus
}: WritableDayProps) {
  const i18n = useTranslation()
  const { present, times } = useFormFields(bind)

  return !present.state ? (
    <FixedSpaceRow fullWidth>
      <LeftCell>{label}</LeftCell>
      <MiddleCell textOnly>{i18n.calendar.reservationModal.absent}</MiddleCell>
      <RightCell>
        <IconButton
          data-qa={dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined}
          icon={fasUserMinus}
          onClick={() => {
            present.set(true)
            times.set([emptyTimeRange])
          }}
          aria-label={i18n.calendar.absentDisable}
        />
      </RightCell>
    </FixedSpaceRow>
  ) : (
    <Times
      bind={times}
      onAbsent={() => present.set(false)}
      label={label}
      showAllErrors={showAllErrors}
      allowExtraTimeRange={allowExtraTimeRange}
      dataQaPrefix={dataQaPrefix}
      onFocus={onFocus}
    />
  )
})

interface ReadOnlyDayProps {
  mode: 'not-editable' | 'holiday' | undefined
  label: React.ReactNode
  dataQaPrefix?: string
}

const ReadOnlyDay = React.memo(function ReadOnlyDay({
  mode,
  label,
  dataQaPrefix
}: ReadOnlyDayProps) {
  const i18n = useTranslation()
  const [infoOpen, setInfoOpen] = useState(false)
  const onInfoClick = useCallback(() => setInfoOpen((prev) => !prev), [])

  switch (mode) {
    case undefined:
      return (
        <FixedSpaceRow fullWidth>
          <LeftCell>{label}</LeftCell>
          <MiddleCell />
          <RightCell />
        </FixedSpaceRow>
      )
    case 'not-editable':
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
    case 'holiday':
      return (
        <FixedSpaceRow fullWidth>
          <LeftCell>{label}</LeftCell>
          <MiddleCell textOnly>{i18n.calendar.holiday}</MiddleCell>
          <RightCell />
        </FixedSpaceRow>
      )
  }
})

interface TimesProps {
  bind: BoundForm<typeof times>
  onAbsent?: () => void
  label: React.ReactNode
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

export const Times = React.memo(function Times({
  bind,
  onAbsent,
  label,
  showAllErrors,
  allowExtraTimeRange,
  dataQaPrefix,
  onFocus
}: TimesProps) {
  const i18n = useTranslation()
  const firstTimeRange = useFormElem(bind, 0)
  const secondTimeRange = useFormElem(bind, 1)

  if (firstTimeRange === undefined) {
    throw new Error('BUG: at least one time range expected')
  }

  return (
    <>
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
            {onAbsent !== undefined ? (
              <IconButton
                data-qa={
                  dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined
                }
                icon={faUserMinus}
                onClick={onAbsent}
                aria-label={i18n.calendar.absentEnable}
              />
            ) : null}
            {secondTimeRange === undefined && allowExtraTimeRange ? (
              <IconButton
                icon={faPlus}
                onClick={() => bind.update((prev) => [prev[0], emptyTimeRange])}
                aria-label={i18n.common.add}
              />
            ) : (
              <div />
            )}
          </FixedSpaceRow>
        </RightCell>
      </FixedSpaceRow>
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
              onClick={() => bind.update((prev) => prev.slice(0, 1))}
              aria-label={i18n.common.delete}
            />
          </RightCell>
        </FixedSpaceRow>
      ) : null}
    </>
  )
})

export function HolidayReservationInputs({
  bind,
  label
}: {
  bind: BoundFormState<boolean>
  label: React.ReactNode
}) {
  return (
    <FixedSpaceRow fullWidth>
      <LeftCell>{label}</LeftCell>
      <MiddleCell>
        <Radio
          checked={bind.state}
          onChange={() => bind.set(true)}
          label="Paikalla"
          ariaLabel="Paikalla"
        />
      </MiddleCell>
      <RightCell>
        <Radio
          checked={!bind.state}
          onChange={() => bind.set(false)}
          label="Poissa"
          ariaLabel="Poissa"
        />
      </RightCell>
    </FixedSpaceRow>
  )
}

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
