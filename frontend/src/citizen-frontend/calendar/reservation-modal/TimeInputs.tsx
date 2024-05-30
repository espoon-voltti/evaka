// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import {
  BoundForm,
  useFormElem,
  useFormField,
  useFormUnion
} from 'lib-common/form/hooks'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { faPlus, fasUserMinus, faTrash, faUserMinus } from 'lib-icons'

import TimeRangeInput from '../TimeRangeInput'

import {
  day,
  emptyTimeRange,
  noTimes,
  timeRanges,
  reservation,
  LimitedLocalTimeRangeField
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
            <IconButton
              data-qa={
                dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined
              }
              icon={fasUserMinus}
              onClick={() => {
                set({
                  branch: 'timeRanges',
                  state: [emptyTimeRange(validTimeRange)]
                })
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
  mode:
    | 'noChildren'
    | 'absentNotEditable'
    | 'termBreak'
    | 'reservationClosed'
    | 'holiday'
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
    case 'noChildren':
      return (
        <FixedSpaceRow fullWidth alignItems="center">
          {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
          <MiddleCell />
          <RightCell />
        </FixedSpaceRow>
      )
    case 'absentNotEditable':
      return (
        <>
          <FixedSpaceRow fullWidth alignItems="center">
            {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
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
                width="full"
              />
            </FixedSpaceRow>
          )}
        </>
      )
    case 'termBreak':
      return (
        <FixedSpaceRow fullWidth alignItems="center">
          {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
          <MiddleCell>{i18n.calendar.termBreak}</MiddleCell>
          <RightCell />
        </FixedSpaceRow>
      )
    case 'reservationClosed':
      return (
        <>
          <FixedSpaceRow fullWidth alignItems="center">
            {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
            <MiddleCell>
              {i18n.calendar.reservationModal.reservationClosed}
            </MiddleCell>
            <RightCell>
              <InfoButton
                onClick={onInfoClick}
                aria-label={i18n.common.openExpandingInfo}
                data-qa="reservation-closed-info-button"
              />
            </RightCell>
          </FixedSpaceRow>
          {infoOpen && (
            <FixedSpaceRow fullWidth>
              <ExpandingInfoBox
                data-qa={
                  dataQaPrefix
                    ? `${dataQaPrefix}-reservation-closed-info-box`
                    : undefined
                }
                close={onInfoClick}
                aria-label={i18n.calendar.reservationModal.reservationClosed}
                info={i18n.calendar.reservationModal.reservationClosedInfo}
                width="full"
              />
            </FixedSpaceRow>
          )}
        </>
      )
    case 'holiday':
      return (
        <FixedSpaceRow fullWidth alignItems="center">
          {label !== undefined ? <LeftCell>{label}</LeftCell> : null}
          <MiddleCell>{i18n.calendar.holiday}</MiddleCell>
          <RightCell />
        </FixedSpaceRow>
      )
  }
})

interface LimitedLocalTimeRangeProps {
  bind: BoundForm<LimitedLocalTimeRangeField>
  hideErrorsBeforeTouched?: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

const LimitedLocalTimeRange = React.memo(function LimitedLocalTimeRange({
  bind,
  hideErrorsBeforeTouched,
  dataQaPrefix,
  onFocus
}: LimitedLocalTimeRangeProps) {
  const value = useFormField(bind, 'value')
  return (
    <TimeRangeInput
      bind={value}
      hideErrorsBeforeTouched={hideErrorsBeforeTouched}
      dataQaPrefix={dataQaPrefix}
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
              <IconButton
                data-qa={
                  dataQaPrefix ? `${dataQaPrefix}-absent-button` : undefined
                }
                icon={faUserMinus}
                onClick={onAbsent}
                aria-label={i18n.calendar.absentEnable}
              />
            ) : null}
            {secondTimeRange === undefined ? (
              <IconButton
                icon={faPlus}
                data-qa={
                  dataQaPrefix ? `${dataQaPrefix}-add-res-button` : undefined
                }
                onClick={() =>
                  bind.update((prev) =>
                    // use same valid range times as first reservation
                    [prev[0], emptyTimeRange(prev[0].validRange)]
                  )
                }
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
