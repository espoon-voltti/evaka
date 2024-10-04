// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import {
  BoundForm,
  useBoolean,
  useFormElem,
  useFormField,
  useFormUnion
} from 'lib-common/form/hooks'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { faPlus, fasUserMinus, faTrash, faUserMinus } from 'lib-icons'

import { focusElementOnNextFrame } from '../../utils/focus'
import TimeRangeInput from '../TimeRangeInput'

import {
  type day,
  emptyTimeRange,
  type noTimes,
  type timeRanges,
  type reservation,
  LimitedLocalTimeRangeField,
  ReadOnlyState
} from './form'

interface DayProps {
  bind: BoundForm<typeof day>
  label: React.ReactNode | undefined
  showAllErrors: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

export const Day = React.memo(function Day({
  bind,
  label,
  showAllErrors,
  dataQaPrefix,
  onFocus
}: DayProps) {
  const { branch, form } = useFormUnion(bind)

  switch (branch) {
    case 'readOnly':
      return (
        <ReadOnlyDay
          state={form.state}
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
          dataQaPrefix={dataQaPrefix}
          onFocus={onFocus}
        />
      )
    case 'reservationNoTimes':
      return <ReservationNoTimes bind={form} label={label} />
  }
})

interface ReservationTimesProps {
  bind: BoundForm<typeof reservation>
  label: React.ReactNode
  showAllErrors: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

const ReservationTimes = React.memo(function ReservationTimes({
  bind,
  label,
  showAllErrors,
  dataQaPrefix,
  onFocus
}: ReservationTimesProps) {
  const i18n = useTranslation()

  const validTimeRange = bind.state.validTimeRange
  const reservation = useFormField(bind, 'reservation')

  const { set } = reservation
  const { branch, form } = useFormUnion(reservation)

  switch (branch) {
    case 'absent':
      return (
        <FixedSpaceRow fullWidth alignItems="center">
          {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
          <MiddleCell>{i18n.calendar.reservationModal.absent}</MiddleCell>
          <RightCell>
            <IconOnlyButton
              id={dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined}
              data-qa={
                dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined
              }
              icon={fasUserMinus}
              onClick={() => {
                set({
                  branch: 'timeRanges',
                  state: [emptyTimeRange(validTimeRange)]
                })
                if (dataQaPrefix) {
                  focusElementOnNextFrame(`${dataQaPrefix}-absent-button`)
                }
              }}
              aria-label={i18n.calendar.absentDisable}
            />
          </RightCell>
        </FixedSpaceRow>
      )
    case 'timeRanges':
      return (
        <TimeRanges
          bind={form}
          label={label}
          showAllErrors={showAllErrors}
          onAbsent={() => set({ branch: 'absent', state: true })}
          dataQaPrefix={dataQaPrefix}
          onFocus={onFocus}
        />
      )
  }
})

interface ReadOnlyDayProps {
  state: ReadOnlyState
  label: React.ReactNode
  dataQaPrefix?: string
}

const ReadOnlyDay = React.memo(function ReadOnlyDay({
  state,
  label,
  dataQaPrefix
}: ReadOnlyDayProps) {
  const i18n = useTranslation()

  const dataQa = (suffix: string) =>
    dataQaPrefix ? `${dataQaPrefix}-${suffix}` : undefined

  switch (state.type) {
    case 'noChildren':
      return (
        <FixedSpaceRow
          fullWidth
          alignItems="center"
          data-qa={dataQa('noChildren')}
        >
          {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
          <MiddleCell />
          <RightCell />
        </FixedSpaceRow>
      )
    case 'absentNotEditable':
      return (
        <WithInfo
          label={label}
          shortText={i18n.calendar.reservationModal.absent}
          longText={i18n.calendar.contactStaffToEditAbsence}
          data-qa={dataQa('absentNotEditable')}
        />
      )
    case 'termBreak':
      return (
        <FixedSpaceRow
          fullWidth
          alignItems="center"
          data-qa={dataQa('termBreak')}
        >
          {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
          <MiddleCell>{i18n.calendar.termBreak}</MiddleCell>
          <RightCell />
        </FixedSpaceRow>
      )
    case 'notYetReservable':
      return (
        <WithInfo
          label={label}
          shortText={i18n.calendar.notYetReservable}
          longText={i18n.calendar.notYetReservableInfo(
            state.period,
            state.reservationsOpenOn
          )}
          data-qa={dataQa('notYetReservable')}
        />
      )
    case 'reservationClosed':
      return (
        <WithInfo
          label={label}
          shortText={i18n.calendar.reservationModal.reservationClosed}
          longText={i18n.calendar.reservationModal.reservationClosedInfo}
          data-qa={dataQa('reservationClosed')}
        />
      )
    case 'holiday':
      return (
        <FixedSpaceRow
          fullWidth
          alignItems="center"
          data-qa={dataQa('holiday')}
        >
          {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
          <MiddleCell>{i18n.calendar.holiday}</MiddleCell>
          <RightCell />
        </FixedSpaceRow>
      )
  }
})

function WithInfo({
  label,
  shortText,
  longText,
  'data-qa': dataQa
}: {
  label?: React.ReactNode
  shortText: string
  longText: string
  'data-qa'?: string
}) {
  const i18n = useTranslation()
  const [infoOpen, useInfoOpen] = useBoolean(false)
  return (
    <>
      <FixedSpaceRow fullWidth alignItems="center" data-qa={dataQa}>
        {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
        <MiddleCell>{shortText}</MiddleCell>
        <RightCell>
          <InfoButton
            onClick={useInfoOpen.toggle}
            aria-label={i18n.common.openExpandingInfo}
          />
        </RightCell>
      </FixedSpaceRow>
      {infoOpen && (
        <FixedSpaceRow fullWidth>
          <ExpandingInfoBox
            close={useInfoOpen.off}
            aria-label={shortText}
            info={longText}
            width="full"
          />
        </FixedSpaceRow>
      )}
    </>
  )
}

interface LimitedLocalTimeRangeProps {
  bind: BoundForm<LimitedLocalTimeRangeField>
  hideErrorsBeforeTouched?: boolean
  dataQaPrefix?: string
  ariaDescribedbyStart?: string
  ariaDescribedbyEnd?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

const LimitedLocalTimeRange = React.memo(function LimitedLocalTimeRange({
  bind,
  hideErrorsBeforeTouched,
  dataQaPrefix,
  ariaDescribedbyStart,
  ariaDescribedbyEnd,
  onFocus
}: LimitedLocalTimeRangeProps) {
  const value = useFormField(bind, 'value')
  return (
    <TimeRangeInput
      bind={value}
      hideErrorsBeforeTouched={hideErrorsBeforeTouched}
      dataQaPrefix={dataQaPrefix}
      ariaDescribedbyStart={ariaDescribedbyStart}
      ariaDescribedbyEnd={ariaDescribedbyEnd}
      onFocus={onFocus}
    />
  )
})

interface TimeRangesProps {
  bind: BoundForm<typeof timeRanges>
  label: React.ReactNode
  showAllErrors: boolean
  dataQaPrefix?: string
  onAbsent?: () => void
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

const TimeRanges = React.memo(function TimeRanges({
  bind,
  label,
  showAllErrors,
  dataQaPrefix,
  onAbsent,
  onFocus
}: TimeRangesProps) {
  const i18n = useTranslation()
  const firstTimeRange = useFormElem(bind, 0)
  const secondTimeRange = useFormElem(bind, 1)

  if (firstTimeRange === undefined) {
    throw new Error('BUG: at least one time range expected')
  }

  return (
    <>
      <FixedSpaceRow fullWidth alignItems="center">
        {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
        <MiddleCell>
          <LimitedLocalTimeRange
            bind={firstTimeRange}
            hideErrorsBeforeTouched={!showAllErrors}
            dataQaPrefix={dataQaPrefix ? `${dataQaPrefix}-time-0` : undefined}
            onFocus={onFocus}
          />
        </MiddleCell>
        <RightCell>
          <FixedSpaceRow>
            {onAbsent !== undefined ? (
              <IconOnlyButton
                id={dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined}
                data-qa={
                  dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined
                }
                icon={faUserMinus}
                onClick={() => {
                  onAbsent()
                  if (dataQaPrefix) {
                    focusElementOnNextFrame(`${dataQaPrefix}-absent-button`)
                  }
                }}
                aria-label={i18n.calendar.absentEnable}
              />
            ) : null}
            {secondTimeRange === undefined ? (
              <IconOnlyButton
                icon={faPlus}
                data-qa={
                  dataQaPrefix ? `${dataQaPrefix}-add-res-button` : undefined
                }
                onClick={() => {
                  bind.update((prev) =>
                    // use same valid range times as first reservation
                    [prev[0], emptyTimeRange(prev[0].validRange)]
                  )
                  if (dataQaPrefix) {
                    focusElementOnNextFrame(`${dataQaPrefix}-time-1-start`)
                  }
                }}
                aria-label={i18n.common.add}
              />
            ) : null}
          </FixedSpaceRow>
        </RightCell>
      </FixedSpaceRow>
      {secondTimeRange !== undefined ? (
        <FixedSpaceRow fullWidth alignItems="center">
          {label !== undefined ? <LeftCell /> : null}
          <MiddleCell>
            <LimitedLocalTimeRange
              bind={secondTimeRange}
              hideErrorsBeforeTouched={!showAllErrors}
              dataQaPrefix={dataQaPrefix ? `${dataQaPrefix}-time-1` : undefined}
              ariaDescribedbyStart={
                i18n.calendar.reservationModal.secondTimeRange.start
              }
              ariaDescribedbyEnd={
                i18n.calendar.reservationModal.secondTimeRange.end
              }
              onFocus={onFocus}
            />
          </MiddleCell>
          <RightCell>
            <IconOnlyButton
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

export const ReservationNoTimes = React.memo(function ReservationNoTimes({
  bind,
  label
}: {
  bind: BoundForm<typeof noTimes>
  label: React.ReactNode
}) {
  const i18n = useTranslation()

  return (
    <FixedSpaceRow fullWidth alignItems="center">
      {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
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
})

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
