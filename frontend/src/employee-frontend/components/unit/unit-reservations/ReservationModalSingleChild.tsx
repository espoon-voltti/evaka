// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { Child } from 'lib-common/api-types/reservations'
import FiniteDateRange from 'lib-common/finite-date-range'
import { boolean, localDateRange, localTimeRange } from 'lib-common/form/fields'
import {
  array,
  chained,
  mapped,
  object,
  oneOf,
  required,
  validated,
  value
} from 'lib-common/form/form'
import {
  BoundForm,
  BoundFormShape,
  useForm,
  useFormElem,
  useFormElems,
  useFormField
} from 'lib-common/form/hooks'
import {
  combine,
  Form,
  StateOf,
  ValidationSuccess
} from 'lib-common/form/types'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { Repetition } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  DatePickerF,
  DatePickerSpacer
} from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { fontWeights, H2, Label, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { Translations } from 'lib-customizations/employee'
import { faPlus, faTrash } from 'lib-icons'

import { postReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'

interface Props {
  onClose: () => void
  onReload: () => void
  child: Child
  isShiftCareUnit: boolean
  operationalDays: number[]
}

const reservableDates = new FiniteDateRange(
  LocalDate.todayInSystemTz(),
  LocalDate.todayInSystemTz().addYears(1)
)

export const times = array(
  validated(required(localTimeRange), ({ endTime }) =>
    // 00:00 is not a valid end time
    endTime.hour === 0 && endTime.minute === 0 ? 'timeFormat' : undefined
  )
)

const weekDay = chained(
  object({
    index: value<number>(),
    enabled: boolean,
    times
  }),
  (form, state) =>
    state.enabled
      ? form.shape.times.validate(state.times).map((times) => ({
          index: state.index,
          times
        }))
      : ValidationSuccess.of(undefined)
)

const irregularDay = object({
  date: value<LocalDate>(),
  times
})

const reservationForm = mapped(
  object({
    dateRange: required(localDateRange),
    repetition: required(oneOf<Repetition>()),
    dailyTimes: times,
    weeklyTimes: mapped(array(weekDay), (output) =>
      output.flatMap((value) => (value !== undefined ? [value] : []))
    ),
    irregularTimes: array(irregularDay)
  }),
  (output) =>
    (childId: UUID): DailyReservationRequest[] => {
      const dates = [...output.dateRange.dates()]
      switch (output.repetition) {
        case 'DAILY':
          return dates.map((date) => ({
            childId,
            date,
            reservations: output.dailyTimes,
            absent: false
          }))
        case 'WEEKLY':
          return dates
            .map((date) => {
              const weekDay = output.weeklyTimes.find(
                (d) => d.index === date.getIsoDayOfWeek()
              )
              return weekDay !== undefined
                ? {
                    childId,
                    date,
                    reservations: weekDay.times,
                    absent: false
                  }
                : undefined
            })
            .flatMap((value) => (value !== undefined ? [value] : []))
        case 'IRREGULAR':
          return output.irregularTimes
            .filter((irregularDay) => {
              return output.dateRange.includes(irregularDay.date)
            })
            .map((irregularDay) => ({
              childId,
              date: irregularDay.date,
              reservations: irregularDay.times,
              absent: false
            }))
      }
    }
)

function repetitionOptions(i18n: Translations) {
  return [
    {
      value: 'DAILY' as const,
      domValue: 'DAILY',
      label: i18n.unit.attendanceReservations.reservationModal.repetitions.DAILY
    },
    {
      value: 'WEEKLY' as const,
      domValue: 'WEEKLY',
      label:
        i18n.unit.attendanceReservations.reservationModal.repetitions.WEEKLY
    },
    {
      value: 'IRREGULAR' as const,
      domValue: 'IRREGULAR',
      label:
        i18n.unit.attendanceReservations.reservationModal.repetitions.IRREGULAR
    }
  ]
}

export function initialState(
  initialStart: LocalDate | null,
  initialEnd: LocalDate | null,
  i18n: Translations
): StateOf<typeof reservationForm> {
  return {
    dateRange: {
      startDate: initialStart,
      endDate: initialEnd
    },
    repetition: {
      domValue: 'DAILY' as const,
      options: repetitionOptions(i18n)
    },
    dailyTimes: [
      {
        startTime: '',
        endTime: ''
      }
    ],
    weeklyTimes: [],
    irregularTimes: []
  }
}

function resetTimes(
  includedWeekDays: number[],
  repetition: Repetition,
  selectedRange: FiniteDateRange
): {
  dailyTimes: StateOf<typeof reservationForm['shape']['dailyTimes']>
  weeklyTimes: StateOf<typeof reservationForm['shape']['weeklyTimes']>
  irregularTimes: StateOf<typeof reservationForm['shape']['irregularTimes']>
} {
  switch (repetition) {
    case 'DAILY':
      return {
        dailyTimes: [{ startTime: '', endTime: '' }],
        weeklyTimes: [],
        irregularTimes: []
      }
    case 'WEEKLY':
      return {
        dailyTimes: [],
        weeklyTimes: includedWeekDays.map((dayIndex) => ({
          index: dayIndex,
          enabled: true,
          times: [{ startTime: '', endTime: '' }]
        })),
        irregularTimes: []
      }
    case 'IRREGULAR':
      return {
        dailyTimes: [],
        weeklyTimes: [],
        irregularTimes: [...selectedRange.dates()].map((date) => ({
          date,
          times: [{ startTime: '', endTime: '' }]
        }))
      }
  }
}

export default React.memo(function ReservationModalSingleChild({
  onClose,
  onReload,
  child,
  isShiftCareUnit,
  operationalDays
}: Props) {
  const { i18n, lang } = useTranslation()

  const includedDays = useMemo(() => {
    return [1, 2, 3, 4, 5, 6, 7].filter((day) => operationalDays.includes(day))
  }, [operationalDays])

  const form = useForm(
    reservationForm,
    () => initialState(LocalDate.todayInSystemTz(), null, i18n),
    i18n.validationErrors,
    {
      onUpdate: (prevState, nextState, form) => {
        const prev = combine(
          form.shape.repetition.validate(prevState.repetition),
          form.shape.dateRange.validate(prevState.dateRange)
        )
        const next = combine(
          form.shape.repetition.validate(nextState.repetition),
          form.shape.dateRange.validate(nextState.dateRange)
        )
        if (!next.isValid) return nextState
        const [repetition, selectedRange] = next.value

        if (!prev.isValid) {
          return {
            ...nextState,
            ...resetTimes(includedDays, repetition, selectedRange)
          }
        }

        const [prevRepetition, prevDateRange] = prev.value
        if (
          selectedRange !== undefined &&
          (prevRepetition !== repetition ||
            prevDateRange === undefined ||
            !prevDateRange.isEqual(selectedRange))
        ) {
          return {
            ...nextState,
            ...resetTimes(includedDays, repetition, selectedRange)
          }
        }
        return nextState
      }
    }
  )

  const repetition = useFormField(form, 'repetition')
  const dateRange = useFormField(form, 'dateRange')
  const startDate = useFormField(dateRange, 'startDate')
  const endDate = useFormField(dateRange, 'endDate')
  const dailyTimes = useFormField(form, 'dailyTimes')
  const weeklyTimes = useFormElems(useFormField(form, 'weeklyTimes'))
  const irregularTimes = useFormElems(useFormField(form, 'irregularTimes'))

  const [showAllErrors, setShowAllErrors] = useState(false)

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.unit.attendanceReservations.reservationModal.title}
      resolveAction={() => {
        if (!form.isValid()) {
          setShowAllErrors(true)
          return
        } else {
          return postReservations(form.value()(child.id))
        }
      }}
      onSuccess={() => {
        onReload()
        onClose()
      }}
      resolveLabel={i18n.common.confirm}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
    >
      <H2>
        {i18n.unit.attendanceReservations.reservationModal.selectedChildren}
      </H2>
      <div>
        {child.lastName} {child.firstName}
      </div>

      <H2>{i18n.unit.attendanceReservations.reservationModal.repetition}</H2>
      <Label>{i18n.common.select}</Label>
      <SelectF bind={repetition} data-qa="repetition" />

      <H2>{i18n.unit.attendanceReservations.reservationModal.dateRange}</H2>
      <Label>
        {i18n.unit.attendanceReservations.reservationModal.dateRangeLabel}
      </Label>
      <FixedSpaceRow>
        <DatePickerF
          bind={startDate}
          data-qa="reservation-start-date"
          locale={lang}
          isInvalidDate={(date) =>
            reservableDates.includes(date)
              ? null
              : i18n.validationErrors.unselectableDate
          }
          minDate={reservableDates.start}
          maxDate={reservableDates.end}
          hideErrorsBeforeTouched={!showAllErrors}
        />
        <DatePickerSpacer />
        <DatePickerF
          bind={endDate}
          data-qa="reservation-end-date"
          locale={lang}
          isInvalidDate={(date) =>
            reservableDates.includes(date)
              ? null
              : i18n.validationErrors.unselectableDate
          }
          minDate={reservableDates.start}
          maxDate={reservableDates.end}
          hideErrorsBeforeTouched={!showAllErrors}
          initialMonth={LocalDate.todayInSystemTz()}
        />
      </FixedSpaceRow>
      <Gap size="m" />

      <TimeInputGrid>
        {repetition.value() === 'DAILY' ? (
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
            bind={dailyTimes}
            showAllErrors={showAllErrors}
            allowExtraTimeRange={isShiftCareUnit}
          />
        ) : repetition.value() === 'WEEKLY' ? (
          weeklyTimes.length > 0 ? (
            weeklyTimes.map((times, index) =>
              includedDays.includes(index + 1) ? (
                <WeeklyTimeInputs
                  key={index}
                  bind={times}
                  index={index}
                  showAllErrors={showAllErrors}
                  allowExtraTimeRange={isShiftCareUnit}
                />
              ) : null
            )
          ) : (
            <MissingDateRange>
              {
                i18n.unit.attendanceReservations.reservationModal
                  .missingDateRange
              }
            </MissingDateRange>
          )
        ) : repetition.value() === 'IRREGULAR' ? (
          irregularTimes.length > 0 ? (
            irregularTimes.map((irregularTimesElem, index) => (
              <IrregularTimeInputs
                key={index}
                bind={irregularTimesElem}
                index={index}
                includedDays={includedDays}
                showAllErrors={showAllErrors}
                allowExtraTimeRange={isShiftCareUnit}
              />
            ))
          ) : (
            <MissingDateRange>
              {
                i18n.unit.attendanceReservations.reservationModal
                  .missingDateRange
              }
            </MissingDateRange>
          )
        ) : null}
      </TimeInputGrid>
    </AsyncFormModal>
  )
})

const WeeklyTimeInputs = React.memo(function WeeklyTimeInputs({
  bind,
  index,
  showAllErrors,
  allowExtraTimeRange
}: {
  bind: BoundForm<typeof weekDay>
  index: number
  showAllErrors: boolean
  allowExtraTimeRange: boolean
}) {
  const { i18n } = useTranslation()

  const enabled = useFormField(bind, 'enabled')
  const times = useFormField(bind, 'times')

  const enabledCheckbox = (
    <CheckboxF
      label={i18n.common.datetime.weekdaysShort[index]}
      bind={enabled}
    />
  )
  return enabled.value() ? (
    <TimeInputs
      key={`day-${index}`}
      label={enabledCheckbox}
      bind={times}
      showAllErrors={showAllErrors}
      allowExtraTimeRange={allowExtraTimeRange}
    />
  ) : (
    <>
      {enabledCheckbox}
      <div />
      <div />
    </>
  )
})

const IrregularTimeInputs = React.memo(function IrregularTimeInputs({
  bind,
  index,
  showAllErrors,
  allowExtraTimeRange,
  includedDays
}: {
  bind: BoundForm<typeof irregularDay>
  index: number
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  includedDays: number[]
}) {
  const { i18n, lang } = useTranslation()

  const date = bind.state.date
  const times = useFormField(bind, 'times')

  return (
    <>
      {index !== 0 && date.getIsoDayOfWeek() === 1 ? <Separator /> : null}
      {index === 0 || date.getIsoDayOfWeek() === 1 ? (
        <Week>
          {i18n.common.datetime.week} {date.getIsoWeek()}
        </Week>
      ) : null}
      {includedDays.includes(date.getIsoDayOfWeek()) && (
        <TimeInputs
          label={<Label>{date.format('EEEEEE d.M.', lang)}</Label>}
          bind={times}
          showAllErrors={showAllErrors}
          allowExtraTimeRange={allowExtraTimeRange}
        />
      )}
    </>
  )
})

const TimeInputs = React.memo(function TimeInputs({
  label,
  bind,
  showAllErrors,
  allowExtraTimeRange
}: {
  label: JSX.Element
  bind: BoundForm<typeof times>
  showAllErrors: boolean
  allowExtraTimeRange: boolean
}) {
  const { i18n } = useTranslation()

  const timeRange = useFormElem(bind, 0)
  const extraTimeRange = useFormElem(bind, 1)

  if (timeRange === undefined) {
    throw new Error('At least one time range expected')
  }

  return (
    <>
      {label}
      <FixedSpaceRow alignItems="center">
        <TimeRangeInput bind={timeRange} showAllErrors={showAllErrors} />
      </FixedSpaceRow>
      {!extraTimeRange && allowExtraTimeRange ? (
        <IconButton
          icon={faPlus}
          data-qa="add-new-reservation-timerange"
          onClick={() =>
            bind.update((prev) => [...prev, { startTime: '', endTime: '' }])
          }
          aria-label={i18n.common.addNew}
        />
      ) : (
        <div />
      )}
      {extraTimeRange ? (
        <>
          <div />
          <FixedSpaceRow alignItems="center">
            <TimeRangeInput
              bind={extraTimeRange}
              showAllErrors={showAllErrors}
            />
          </FixedSpaceRow>
          <IconButton
            icon={faTrash}
            onClick={() => bind.update((prev) => prev.slice(0, 1))}
            aria-label={i18n.common.remove}
          />
        </>
      ) : null}
    </>
  )
})

const TimeRangeInput = React.memo(function TimeRangeInput({
  bind,
  showAllErrors
}: {
  bind: BoundFormShape<
    { startTime: string; endTime: string },
    {
      startTime: Form<unknown, string, string, unknown>
      endTime: Form<unknown, string, string, unknown>
    }
  >
  showAllErrors: boolean
}) {
  const startTime = useFormField(bind, 'startTime')
  const endTime = useFormField(bind, 'endTime')
  return (
    <>
      <TimeInputF
        data-qa="reservation-start-time"
        bind={startTime}
        hideErrorsBeforeTouched={!showAllErrors}
      />
      <span>–</span>
      <TimeInputF
        data-qa="reservation-end-time"
        bind={endTime}
        hideErrorsBeforeTouched={!showAllErrors}
      />
    </>
  )
})

const TimeInputGrid = styled.div`
  display: grid;
  grid-template-columns: max-content max-content auto;
  grid-column-gap: ${defaultMargins.s};
  grid-row-gap: ${defaultMargins.s};
  align-items: center;
`

const Week = styled.div`
  color: ${(p) => p.theme.colors.main.m1};
  font-weight: ${fontWeights.semibold};
  grid-column-start: 1;
  grid-column-end: 4;
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.grayscale.g15};
  margin: ${defaultMargins.s} 0;
  grid-column-start: 1;
  grid-column-end: 4;
`

const MissingDateRange = styled(Light)`
  grid-column-start: 1;
  grid-column-end: 4;
`
