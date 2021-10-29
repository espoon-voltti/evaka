// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, H2, Label } from 'lib-components/typography'
import React, { Fragment, useMemo, useState } from 'react'
import styled from 'styled-components'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { useLang, useTranslation } from '../localization'
import { postReservations } from './api'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { SelectionChip } from 'lib-components/atoms/Chip'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import LocalDate from 'lib-common/local-date'
import DatePicker, {
  DatePickerSpacer
} from 'lib-components/molecules/date-picker/DatePicker'
import { UUID } from 'lib-common/types'
import { ErrorKey, regexp, TIME_REGEXP } from 'lib-common/form-validation'
import FiniteDateRange from 'lib-common/finite-date-range'
import Combobox from 'lib-components/atoms/form/Combobox'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { errorToInputInfo } from '../input-info-helper'
import {
  DailyReservationRequest,
  ReservationChild,
  TimeRange
} from 'lib-common/generated/api-types/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faPlus, faTrash } from 'lib-icons'

interface Props {
  onClose: () => void
  onReload: () => void
  availableChildren: ReservationChild[]
  reservableDays: FiniteDateRange
}

type Repetition = 'DAILY' | 'WEEKLY' | 'IRREGULAR'

interface ReservationFormData {
  selectedChildren: UUID[]
  startDate: string
  endDate: string
  repetition: Repetition
  dailyTimes: TimeRanges
  weeklyTimes: Array<TimeRanges | undefined>
  irregularTimes: Record<string, TimeRanges | undefined>
}

type TimeRanges = [TimeRange] | [TimeRange, TimeRange]
type TimeRangeErrors = {
  startTime: ErrorKey | undefined
  endTime: ErrorKey | undefined
}

type ReservationErrors = Partial<
  Record<
    keyof Omit<
      ReservationFormData,
      'dailyTimes' | 'weeklyTimes' | 'irregularTimes'
    >,
    ErrorKey
  > & {
    dailyTimes: TimeRangeErrors[]
  } & {
    weeklyTimes: Array<TimeRangeErrors[] | undefined>
  } & {
    irregularTimes: Record<string, TimeRangeErrors[] | undefined>
  }
>

export default React.memo(function ReservationModal({
  onClose,
  onReload,
  availableChildren,
  reservableDays
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [formData, setFormData] = useState<ReservationFormData>({
    selectedChildren: availableChildren.map((child) => child.id),
    startDate: reservableDays.start.format(),
    endDate: '',
    repetition: 'DAILY',
    dailyTimes: [
      {
        startTime: '',
        endTime: ''
      }
    ],
    weeklyTimes: [0, 1, 2, 3, 4, 5, 6].map(() => [
      {
        startTime: '',
        endTime: ''
      }
    ]),
    irregularTimes: {}
  })

  const updateForm = (updated: Partial<ReservationFormData>) => {
    setFormData((prev) => ({
      ...prev,
      ...updated
    }))
  }

  const [showAllErrors, setShowAllErrors] = useState(false)
  const validationResult = useMemo(
    () => validateForm(reservableDays, formData),
    [reservableDays, formData]
  )

  const shiftCareRange = useMemo(() => {
    if (formData.repetition !== 'IRREGULAR') return

    const parsedStartDate = LocalDate.parseFiOrNull(formData.startDate)
    const parsedEndDate = LocalDate.parseFiOrNull(formData.endDate)

    if (
      !parsedStartDate ||
      !parsedEndDate ||
      parsedEndDate.isBefore(parsedStartDate)
    ) {
      return
    }

    return [...new FiniteDateRange(parsedStartDate, parsedEndDate).dates()]
  }, [formData.repetition, formData.startDate, formData.endDate])

  const childrenInShiftCare = useMemo(
    () => availableChildren.some(({ inShiftCareUnit }) => inShiftCareUnit),
    [availableChildren]
  )

  const includedDays = useMemo(() => {
    const combinedOperationDays = availableChildren.reduce<number[]>(
      (totalOperationDays, child) => [
        ...new Set([...totalOperationDays, ...child.maxOperationalDays])
      ],
      []
    )

    return [1, 2, 3, 4, 5, 6, 7].filter((day) =>
      combinedOperationDays.includes(day)
    )
  }, [availableChildren])

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.calendar.reservationModal.title}
      resolve={{
        action: (cancel) => {
          if (validationResult.errors) {
            setShowAllErrors(true)
            return cancel()
          } else {
            return postReservations(validationResult.requestPayload)
          }
        },
        onSuccess: () => {
          onReload()
          onClose()
        },
        label: i18n.common.confirm,
        disabled: formData.selectedChildren.length === 0
      }}
      reject={{
        action: onClose,
        label: i18n.common.cancel
      }}
    >
      <H2>{i18n.calendar.reservationModal.selectChildren}</H2>
      <Label>{i18n.calendar.reservationModal.selectChildrenLabel}</Label>
      <Gap size="xs" />
      <FixedSpaceFlexWrap>
        {availableChildren.map((child) => (
          <SelectionChip
            key={child.id}
            text={child.preferredName || child.firstName.split(' ')[0]}
            selected={formData.selectedChildren.includes(child.id)}
            onChange={(selected) => {
              if (selected) {
                updateForm({
                  selectedChildren: [...formData.selectedChildren, child.id]
                })
              } else {
                updateForm({
                  selectedChildren: formData.selectedChildren.filter(
                    (id) => id !== child.id
                  )
                })
              }
            }}
            data-qa={`child-${child.id}`}
          />
        ))}
      </FixedSpaceFlexWrap>

      <H2>{i18n.calendar.reservationModal.repetition}</H2>
      <Label>{i18n.common.select}</Label>
      <Combobox<Repetition>
        items={['DAILY', 'WEEKLY', 'IRREGULAR']}
        selectedItem={formData.repetition}
        onChange={(value) => {
          if (value) updateForm({ repetition: value })
        }}
        clearable={false}
        getItemLabel={(item) =>
          i18n.calendar.reservationModal.repetitions[item]
        }
        data-qa="repetition"
      />

      <H2>{i18n.calendar.reservationModal.dateRange}</H2>
      <Label>{i18n.calendar.reservationModal.dateRangeLabel}</Label>
      <FixedSpaceRow>
        <DatePicker
          date={formData.startDate}
          onChange={(date) => updateForm({ startDate: date })}
          locale={lang}
          isValidDate={(date) => reservableDays.includes(date)}
          info={errorToInputInfo(
            validationResult.errors?.startDate,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!showAllErrors}
          data-qa="start-date"
        />
        <DatePickerSpacer />
        <DatePicker
          date={formData.endDate}
          onChange={(date) => updateForm({ endDate: date })}
          locale={lang}
          isValidDate={(date) => reservableDays.includes(date)}
          info={errorToInputInfo(
            validationResult.errors?.endDate,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!showAllErrors}
          initialMonth={
            LocalDate.parseFiOrNull(formData.startDate) ?? reservableDays.start
          }
          data-qa="end-date"
        />
      </FixedSpaceRow>
      <Gap size="m" />

      <TimeInputGrid>
        {formData.repetition === 'DAILY' && (
          <TimeInputs
            label={
              <Label>{`${
                i18n.common.datetime.weekdaysShort[includedDays[0] - 1]
              }-${
                i18n.common.datetime.weekdaysShort[
                  includedDays[includedDays.length - 1] - 1
                ]
              }`}</Label>
            }
            times={formData.dailyTimes}
            updateTimes={(dailyTimes) =>
              updateForm({
                dailyTimes
              })
            }
            validationErrors={validationResult.errors?.dailyTimes}
            showAllErrors={showAllErrors}
            allowExtraTimeRange={childrenInShiftCare}
            dataQaPrefix="daily"
          />
        )}

        {formData.repetition === 'WEEKLY' &&
          formData.weeklyTimes.map((times, index) =>
            includedDays.includes(index + 1) ? (
              <TimeInputs
                key={`day-${index}`}
                label={
                  <Checkbox
                    label={i18n.common.datetime.weekdaysShort[index]}
                    checked={!!times}
                    onChange={(checked) =>
                      updateForm({
                        weeklyTimes: [
                          ...formData.weeklyTimes.slice(0, index),
                          checked
                            ? [
                                {
                                  startTime: '',
                                  endTime: ''
                                }
                              ]
                            : undefined,
                          ...formData.weeklyTimes.slice(index + 1)
                        ]
                      })
                    }
                  />
                }
                times={times}
                updateTimes={(times) =>
                  updateForm({
                    weeklyTimes: [
                      ...formData.weeklyTimes.slice(0, index),
                      times,
                      ...formData.weeklyTimes.slice(index + 1)
                    ]
                  })
                }
                validationErrors={validationResult.errors?.weeklyTimes?.[index]}
                showAllErrors={showAllErrors}
                allowExtraTimeRange={childrenInShiftCare}
                dataQaPrefix={`weekly-${index}`}
              />
            ) : null
          )}

        {formData.repetition === 'IRREGULAR' ? (
          shiftCareRange ? (
            shiftCareRange.map((date, index) => (
              <Fragment key={`shift-care-${date.formatIso()}`}>
                {index !== 0 && date.getIsoDayOfWeek() === 1 ? (
                  <Separator />
                ) : null}
                {index === 0 || date.getIsoDayOfWeek() === 1 ? (
                  <Week>
                    {i18n.common.datetime.week} {date.getIsoWeek()}
                  </Week>
                ) : null}
                {includedDays.includes(date.getIsoDayOfWeek()) && (
                  <TimeInputs
                    label={
                      <Label>
                        {`${
                          i18n.common.datetime.weekdaysShort[
                            date.getIsoDayOfWeek() - 1
                          ]
                        } ${date.format('d.M.')}`}
                      </Label>
                    }
                    times={
                      formData.irregularTimes[date.formatIso()] ?? [
                        {
                          startTime: '',
                          endTime: ''
                        }
                      ]
                    }
                    updateTimes={(times) =>
                      updateForm({
                        irregularTimes: {
                          ...formData.irregularTimes,
                          [date.formatIso()]: times
                        }
                      })
                    }
                    validationErrors={
                      validationResult.errors?.irregularTimes?.[
                        date.formatIso()
                      ]
                    }
                    showAllErrors={showAllErrors}
                    allowExtraTimeRange={childrenInShiftCare}
                    dataQaPrefix={`irregular-${date.formatIso()}`}
                  />
                )}
              </Fragment>
            ))
          ) : (
            <MissingDateRange>
              {i18n.calendar.reservationModal.missingDateRange}
            </MissingDateRange>
          )
        ) : null}
      </TimeInputGrid>
    </AsyncFormModal>
  )
})

const TimeInputs = React.memo(function TimeInputs(props: {
  label: JSX.Element
  times: TimeRanges | undefined
  updateTimes: (v: TimeRanges | undefined) => void
  validationErrors: TimeRangeErrors[] | undefined
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
}) {
  const i18n = useTranslation()

  if (!props.times) {
    return (
      <>
        {props.label}
        <div />
        <div />
      </>
    )
  }

  const [timeRange, extraTimeRange] = props.times
  return (
    <>
      {props.label}
      <FixedSpaceRow alignItems="center">
        <TimeInput
          value={timeRange.startTime ?? ''}
          onChange={(value) => {
            const updatedRange = {
              startTime: value,
              endTime: timeRange.endTime ?? ''
            }

            props.updateTimes(
              extraTimeRange ? [updatedRange, extraTimeRange] : [updatedRange]
            )
          }}
          info={errorToInputInfo(
            props.validationErrors?.[0]?.startTime,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!props.showAllErrors}
          placeholder={i18n.calendar.reservationModal.start}
          data-qa={
            props.dataQaPrefix
              ? `${props.dataQaPrefix}-start-time-0`
              : undefined
          }
        />
        <span>–</span>
        <TimeInput
          value={timeRange.endTime ?? ''}
          onChange={(value) => {
            const updatedRange = {
              startTime: timeRange.startTime ?? '',
              endTime: value
            }

            props.updateTimes(
              extraTimeRange ? [updatedRange, extraTimeRange] : [updatedRange]
            )
          }}
          info={errorToInputInfo(
            props.validationErrors?.[0]?.endTime,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!props.showAllErrors}
          placeholder={i18n.calendar.reservationModal.end}
          data-qa={
            props.dataQaPrefix ? `${props.dataQaPrefix}-end-time-0` : undefined
          }
        />
      </FixedSpaceRow>
      {!extraTimeRange && props.allowExtraTimeRange ? (
        <IconButton
          icon={faPlus}
          onClick={() =>
            props.updateTimes([
              timeRange,
              {
                startTime: '',
                endTime: ''
              }
            ])
          }
        />
      ) : (
        <div />
      )}
      {extraTimeRange ? (
        <>
          <div />
          <FixedSpaceRow alignItems="center">
            <TimeInput
              value={extraTimeRange.startTime ?? ''}
              onChange={(value) =>
                props.updateTimes([
                  timeRange,
                  {
                    startTime: value,
                    endTime: extraTimeRange.endTime ?? ''
                  }
                ])
              }
              info={errorToInputInfo(
                props.validationErrors?.[1]?.startTime,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={!props.showAllErrors}
              placeholder={i18n.calendar.reservationModal.start}
              data-qa={
                props.dataQaPrefix
                  ? `${props.dataQaPrefix}-start-time-1`
                  : undefined
              }
            />
            <span>–</span>
            <TimeInput
              value={extraTimeRange.endTime ?? ''}
              onChange={(value) =>
                props.updateTimes([
                  timeRange,
                  {
                    startTime: extraTimeRange.startTime ?? '',
                    endTime: value
                  }
                ])
              }
              info={errorToInputInfo(
                props.validationErrors?.[1]?.endTime,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={!props.showAllErrors}
              placeholder={i18n.calendar.reservationModal.end}
              data-qa={
                props.dataQaPrefix
                  ? `${props.dataQaPrefix}-end-time-1`
                  : undefined
              }
            />
          </FixedSpaceRow>
          <IconButton
            icon={faTrash}
            onClick={() => props.updateTimes([timeRange])}
          />
        </>
      ) : null}
    </>
  )
})

type ValidationResult =
  | { errors: ReservationErrors }
  | { errors: undefined; requestPayload: DailyReservationRequest[] }

function validateForm(
  reservableDays: FiniteDateRange,
  formData: ReservationFormData
): ValidationResult {
  const errors: ReservationErrors = {}

  if (formData.selectedChildren.length < 1) {
    errors['selectedChildren'] = 'required'
  }

  const startDate = LocalDate.parseFiOrNull(formData.startDate)
  if (startDate === null) {
    errors['startDate'] = 'validDate'
  } else if (startDate.isBefore(reservableDays.start)) {
    errors['startDate'] = 'dateTooEarly'
  }

  const endDate = LocalDate.parseFiOrNull(formData.endDate)
  if (endDate === null) {
    errors['endDate'] = 'validDate'
  } else if (startDate && endDate.isBefore(startDate)) {
    errors['endDate'] = 'dateTooEarly'
  } else if (endDate.isAfter(reservableDays.end)) {
    errors['endDate'] = 'dateTooLate'
  }

  if (formData.repetition === 'DAILY') {
    errors['dailyTimes'] = formData.dailyTimes.map((time) => ({
      startTime:
        time.startTime === ''
          ? time.endTime !== ''
            ? 'required'
            : undefined
          : regexp(time.startTime, TIME_REGEXP, 'timeFormat'),
      endTime:
        time.endTime === ''
          ? time.startTime !== ''
            ? 'required'
            : undefined
          : regexp(time.endTime, TIME_REGEXP, 'timeFormat')
    }))
  }

  if (formData.repetition === 'WEEKLY') {
    errors['weeklyTimes'] = formData.weeklyTimes.map((times) =>
      times
        ? times.map((time) => ({
            startTime:
              time.startTime === ''
                ? time.endTime !== ''
                  ? 'required'
                  : undefined
                : regexp(time.startTime, TIME_REGEXP, 'timeFormat'),
            endTime:
              time.endTime === ''
                ? time.startTime !== ''
                  ? 'required'
                  : undefined
                : regexp(time.endTime, TIME_REGEXP, 'timeFormat')
          }))
        : undefined
    )
  }

  if (formData.repetition === 'IRREGULAR') {
    errors['irregularTimes'] = Object.fromEntries(
      Object.entries(formData.irregularTimes).map(([date, times]) => [
        date,
        times
          ? times.map((time) => ({
              startTime:
                time.startTime === ''
                  ? time.endTime !== ''
                    ? 'required'
                    : undefined
                  : regexp(time.startTime, TIME_REGEXP, 'timeFormat'),
              endTime:
                time.endTime === ''
                  ? time.startTime !== ''
                    ? 'required'
                    : undefined
                  : regexp(time.endTime, TIME_REGEXP, 'timeFormat')
            }))
          : undefined
      ])
    )
  }

  if (errorsExist(errors)) {
    return { errors }
  }

  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const dateRange = new FiniteDateRange(startDate!, endDate!)
  const dates = [...dateRange.dates()]

  return {
    errors: undefined,
    requestPayload: formData.selectedChildren.flatMap((childId) => {
      switch (formData.repetition) {
        case 'DAILY':
          return dates.map((date) => ({
            childId,
            date,
            reservations: filterEmptyReservationTimes(formData.dailyTimes)
          }))
        case 'WEEKLY':
          return dates.map((date) => ({
            childId,
            date,
            reservations: filterEmptyReservationTimes(
              formData.weeklyTimes[date.getIsoDayOfWeek() - 1]
            )
          }))
        case 'IRREGULAR':
          return Object.entries(formData.irregularTimes)
            .filter(([isoDate]) => {
              const date = LocalDate.tryParseIso(isoDate)
              return date && dateRange.includes(date)
            })
            .map(([isoDate, times]) => ({
              childId,
              date: LocalDate.parseIso(isoDate),
              reservations: filterEmptyReservationTimes(times)
            }))
      }
    })
  }
}

function filterEmptyReservationTimes(times: TimeRanges | undefined) {
  return times?.filter(({ startTime, endTime }) => startTime && endTime) ?? null
}

function errorsExist(errors: ReservationErrors): boolean {
  const {
    dailyTimes: dailyErrors,
    weeklyTimes: weeklyErrors,
    irregularTimes: shiftCareErrors,
    ...otherErrors
  } = errors

  for (const error of Object.values(otherErrors)) {
    if (error) return true
  }

  if (dailyErrors?.some((error) => error.startTime || error.endTime)) {
    return true
  }

  for (const errors of weeklyErrors ?? []) {
    if (errors?.some((error) => error.startTime || error.endTime)) return true
  }

  for (const errors of Object.values(shiftCareErrors ?? {})) {
    if (errors?.some((error) => error.startTime || error.endTime)) return true
  }

  return false
}

const TimeInputGrid = styled.div`
  display: grid;
  grid-template-columns: max-content max-content auto;
  grid-column-gap: ${defaultMargins.s};
  grid-row-gap: ${defaultMargins.s};
  align-items: center;
`

const Week = styled.div`
  color: ${({ theme }) => theme.colors.main.dark};
  font-weight: ${fontWeights.semibold};
  grid-column-start: 1;
  grid-column-end: 4;
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.greyscale.lighter};
  margin: ${defaultMargins.s} 0;
  grid-column-start: 1;
  grid-column-end: 4;
`

const MissingDateRange = styled.span`
  color: ${({ theme }) => theme.colors.greyscale.dark};
  font-style: italic;
  grid-column-start: 1;
  grid-column-end: 4;
`
