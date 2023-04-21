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

import { day, emptyTimeRange, times } from './form'

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

  switch (branch) {
    case 'readOnly':
      return (
        <ReadOnlyDay
          mode={form.state}
          label={label}
          dataQaPrefix={dataQaPrefix}
        />
      )
    case 'reservation':
      return (
        <ReservationTimes
          bind={form}
          label={label}
          showAllErrors={showAllErrors}
          allowExtraTimeRange={allowExtraTimeRange}
          dataQaPrefix={dataQaPrefix}
          onFocus={onFocus}
        />
      )
    case 'holidayReservation':
      return <HolidayReservation bind={form} label={label} />
  }
})

interface ReservationTimesProps {
  bind: BoundForm<typeof times>
  label: React.ReactNode
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

const ReservationTimes = React.memo(function ReservationTimes({
  bind,
  label,
  showAllErrors,
  allowExtraTimeRange,
  dataQaPrefix,
  onFocus
}: ReservationTimesProps) {
  const i18n = useTranslation()
  const { set, state } = bind

  // Empty reservations array => absent
  return state.length === 0 ? (
    <FixedSpaceRow fullWidth alignItems="center">
      <LeftCell>{label}</LeftCell>
      <MiddleCell>{i18n.calendar.reservationModal.absent}</MiddleCell>
      <RightCell>
        <IconButton
          data-qa={dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined}
          icon={fasUserMinus}
          onClick={() => {
            set([emptyTimeRange])
          }}
          aria-label={i18n.calendar.absentDisable}
        />
      </RightCell>
    </FixedSpaceRow>
  ) : (
    <Times
      bind={bind}
      label={label}
      showAllErrors={showAllErrors}
      allowAbsence
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
        <FixedSpaceRow fullWidth alignItems="center">
          <LeftCell>{label}</LeftCell>
          <MiddleCell />
          <RightCell />
        </FixedSpaceRow>
      )
    case 'not-editable':
      return (
        <>
          <FixedSpaceRow fullWidth alignItems="center">
            <LeftCell>{label}</LeftCell>
            <MiddleCell>{i18n.calendar.reservationModal.absent}</MiddleCell>
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
        <FixedSpaceRow fullWidth alignItems="center">
          <LeftCell>{label}</LeftCell>
          <MiddleCell>{i18n.calendar.holiday}</MiddleCell>
          <RightCell />
        </FixedSpaceRow>
      )
  }
})

interface TimesProps {
  bind: BoundForm<typeof times>
  label: React.ReactNode
  showAllErrors: boolean
  allowAbsence?: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

export const Times = React.memo(function Times({
  bind,
  label,
  showAllErrors,
  allowAbsence = false,
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
      <FixedSpaceRow fullWidth alignItems="center">
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
            {allowAbsence ? (
              <IconButton
                data-qa={
                  dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined
                }
                icon={faUserMinus}
                onClick={() => bind.set([])}
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
        <FixedSpaceRow fullWidth alignItems="center">
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

export function HolidayReservation({
  bind,
  label
}: {
  bind: BoundFormState<'present' | 'absent'>
  label: React.ReactNode
}) {
  const i18n = useTranslation()
  return (
    <FixedSpaceRow fullWidth alignItems="center">
      <LeftCell>{label}</LeftCell>
      <MiddleCell narrow>
        <Radio
          checked={bind.state === 'present'}
          onChange={() => bind.set('present')}
          label={i18n.calendar.reservationModal.present}
          ariaLabel={i18n.calendar.reservationModal.present}
        />
      </MiddleCell>
      <RightCell>
        <Radio
          checked={bind.state === 'absent'}
          onChange={() => bind.set('absent')}
          label={i18n.calendar.reservationModal.absent}
          ariaLabel={i18n.calendar.reservationModal.absent}
        />
      </RightCell>
    </FixedSpaceRow>
  )
}

const LeftCell = styled.div`
  width: 80px;
`
const MiddleCell = styled.div<{ narrow?: boolean }>`
  display: inline-flex;
  align-items: center;
  flex: ${(p) => (p.narrow ? 1 : 3)};
  min-height: 37px;
`
const RightCell = styled.div`
  display: inline-flex;
  align-items: center;
  flex: 2;
  min-height: 37px;
`
