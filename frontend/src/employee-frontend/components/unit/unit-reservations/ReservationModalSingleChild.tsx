// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faUserMinus } from 'Icons'
import classNames from 'classnames'
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
  union,
  value
} from 'lib-common/form/form'
import {
  BoundForm,
  BoundFormShape,
  useForm,
  useFormElem,
  useFormElems,
  useFormField,
  useFormUnion
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
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { cancelMutation } from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { fontWeights, H2, Label, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { Translations } from 'lib-customizations/employee'
import { faPlus, faTrash } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { postReservationsMutation } from '../queries'

interface Props {
  onClose: () => void
  child: Child
  operationalDays: number[]
}

const reservableDates = new FiniteDateRange(
  LocalDate.todayInSystemTz(),
  LocalDate.todayInSystemTz().addYears(1)
)
const ranges = array(required(localTimeRange()))
export const reservation = union({
  timeRanges: ranges,
  absent: value<true>()
})

const weekDay = chained(
  object({
    index: value<number>(),
    enabled: boolean(),
    times: reservation
  }),
  (form, state) =>
    state.enabled
      ? form
          .shape()
          .times.validate(state.times)
          .map((times) => ({
            index: state.index,
            times
          }))
      : ValidationSuccess.of(undefined)
)

const irregularDay = object({
  date: value<LocalDate>(),
  times: reservation
})

const reservationForm = mapped(
  object({
    dateRange: required(localDateRange()),
    repetition: required(oneOf<Repetition>()),
    dailyTimes: reservation,
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
          return dates.map((date) => {
            if (output.dailyTimes.branch === 'absent') {
              return { type: 'ABSENT', childId, date }
            }
            return {
              type: 'RESERVATIONS',
              childId,
              date,
              reservation: output.dailyTimes.value[0],
              secondReservation: output.dailyTimes.value[1] ?? null
            }
          })
        case 'WEEKLY':
          return dates
            .map((date) => {
              const weekDay = output.weeklyTimes.find(
                (d) => d.index === date.getIsoDayOfWeek()
              )
              if (weekDay === undefined) {
                return undefined
              }
              if (weekDay.times.branch === 'absent') {
                return { type: 'ABSENT' as const, childId, date }
              }
              return {
                type: 'RESERVATIONS' as const,
                childId,
                date,
                reservation: weekDay.times.value[0],
                secondReservation: weekDay.times.value[1] ?? null
              }
            })
            .flatMap((value) => (value !== undefined ? [value] : []))
        case 'IRREGULAR':
          return output.irregularTimes
            .filter((irregularDay) =>
              output.dateRange.includes(irregularDay.date)
            )
            .map((irregularDay) => {
              if (irregularDay.times.branch === 'absent') {
                return {
                  type: 'ABSENT' as const,
                  childId,
                  date: irregularDay.date
                }
              }
              return {
                type: 'RESERVATIONS' as const,
                childId,
                date: irregularDay.date,
                reservation: irregularDay.times.value[0],
                secondReservation: irregularDay.times.value[1] ?? null
              }
            })
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
    dateRange: localDateRange.fromDates(initialStart, initialEnd, {
      minDate: reservableDates.start,
      maxDate: reservableDates.end
    }),
    repetition: {
      domValue: 'DAILY' as const,
      options: repetitionOptions(i18n)
    },
    dailyTimes: {
      branch: 'timeRanges',
      state: [
        {
          startTime: '',
          endTime: ''
        }
      ]
    },
    weeklyTimes: [],
    irregularTimes: []
  }
}

function resetTimes(
  includedWeekDays: number[],
  repetition: Repetition,
  selectedRange: FiniteDateRange
): {
  dailyTimes: StateOf<
    ReturnType<(typeof reservationForm)['shape']>['dailyTimes']
  >
  weeklyTimes: StateOf<
    ReturnType<(typeof reservationForm)['shape']>['weeklyTimes']
  >
  irregularTimes: StateOf<
    ReturnType<(typeof reservationForm)['shape']>['irregularTimes']
  >
} {
  switch (repetition) {
    case 'DAILY':
      return {
        dailyTimes: {
          branch: 'timeRanges',
          state: [{ startTime: '', endTime: '' }]
        },
        weeklyTimes: [],
        irregularTimes: []
      }
    case 'WEEKLY':
      return {
        dailyTimes: {
          branch: 'timeRanges',
          state: []
        },
        weeklyTimes: includedWeekDays.map((dayIndex) => ({
          index: dayIndex,
          enabled: true,
          times: {
            branch: 'timeRanges',
            state: [{ startTime: '', endTime: '' }]
          }
        })),
        irregularTimes: []
      }
    case 'IRREGULAR':
      return {
        dailyTimes: {
          branch: 'timeRanges',
          state: []
        },
        weeklyTimes: [],
        irregularTimes: [...selectedRange.dates()].map((date) => ({
          date,
          times: {
            branch: 'timeRanges',
            state: [{ startTime: '', endTime: '' }]
          }
        }))
      }
  }
}

export default React.memo(function ReservationModalSingleChild({
  onClose,
  child,
  operationalDays
}: Props) {
  const { i18n, lang } = useTranslation()

  const includedDays = useMemo(
    () => [1, 2, 3, 4, 5, 6, 7].filter((day) => operationalDays.includes(day)),
    [operationalDays]
  )

  const form = useForm(
    reservationForm,
    () => initialState(LocalDate.todayInSystemTz(), null, i18n),
    i18n.validationErrors,
    {
      onUpdate: (prevState, nextState, form) => {
        const shape = form.shape()
        const prev = combine(
          shape.repetition.validate(prevState.repetition),
          shape.dateRange.validate(prevState.dateRange)
        )
        const next = combine(
          shape.repetition.validate(nextState.repetition),
          shape.dateRange.validate(nextState.dateRange)
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
  const dailyTimes = useFormField(form, 'dailyTimes')
  const weeklyTimes = useFormElems(useFormField(form, 'weeklyTimes'))
  const irregularTimes = useFormElems(useFormField(form, 'irregularTimes'))

  const [showAllErrors, setShowAllErrors] = useState(false)

  return (
    <MutateFormModal
      mobileFullScreen
      title={i18n.unit.attendanceReservations.reservationModal.title}
      resolveMutation={postReservationsMutation}
      resolveAction={() => {
        if (!form.isValid()) {
          setShowAllErrors(true)
          return cancelMutation
        } else {
          return form.value()(child.id)
        }
      }}
      onSuccess={onClose}
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
      <DateRangePickerF
        bind={dateRange}
        locale={lang}
        hideErrorsBeforeTouched={!showAllErrors}
        data-qa="reservation-date-range"
      />
      <Gap size="m" />

      <TimeInputGrid>
        {repetition.value() === 'DAILY' ? (
          <ReservationTimes
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
    </MutateFormModal>
  )
})

const WeeklyTimeInputs = React.memo(function WeeklyTimeInputs({
  bind,
  index,
  showAllErrors
}: {
  bind: BoundForm<typeof weekDay>
  index: number
  showAllErrors: boolean
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
    <ReservationTimes
      key={`day-${index}`}
      label={enabledCheckbox}
      bind={times}
      showAllErrors={showAllErrors}
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
  includedDays
}: {
  bind: BoundForm<typeof irregularDay>
  index: number
  showAllErrors: boolean
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
        <ReservationTimes
          label={<Label>{date.format('EEEEEE d.M.', lang)}</Label>}
          bind={times}
          showAllErrors={showAllErrors}
        />
      )}
    </>
  )
})

const TimeInputs = React.memo(function TimeInputs({
  label,
  bind,
  showAllErrors,
  onAbsent
}: {
  label: React.JSX.Element
  bind: BoundForm<typeof ranges>
  showAllErrors: boolean
  onAbsent: () => void
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
      <TimeRangeInput bind={timeRange} showAllErrors={showAllErrors} />
      <DayButtons>
        <IconButton
          icon={faUserMinus}
          data-qa="set-absent-button"
          onClick={onAbsent}
          aria-label="Merkitse poissaolevaksi"
        />
        {!extraTimeRange ? (
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
      </DayButtons>
      {extraTimeRange ? (
        <>
          <div />
          <TimeRangeInput bind={extraTimeRange} showAllErrors={showAllErrors} />
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

const ReservationTimes = React.memo(function ReservationTimes({
  label,
  bind,
  showAllErrors
}: {
  label: React.JSX.Element
  bind: BoundForm<typeof reservation>
  showAllErrors: boolean
}) {
  const { branch, form } = useFormUnion(bind)

  const onAbsent = () => {
    bind.update(() => ({
      branch: 'absent',
      state: true
    }))
  }

  switch (branch) {
    case 'absent':
      return (
        <>
          {label}
          <span>Poissa</span>
          <div>
            <IconButton
              icon={faUserMinus}
              data-qa="set-present-button"
              onClick={() =>
                bind.update(() => ({
                  branch: 'timeRanges',
                  state: [{ startTime: '', endTime: '' }]
                }))
              }
              aria-label="Merkitse poissaolevaksi"
            />
          </div>
        </>
      )
    case 'timeRanges':
      return (
        <TimeInputs
          label={label}
          bind={form}
          showAllErrors={showAllErrors}
          onAbsent={onAbsent}
        />
      )
  }
})

const TimeRangeInput = React.memo(function TimeRangeInput({
  bind,
  showAllErrors
}: {
  bind: BoundFormShape<
    {
      startTime: string
      endTime: string
    },
    {
      startTime: Form<unknown, string, string, unknown>
      endTime: Form<unknown, string, string, unknown>
    }
  >
  showAllErrors: boolean
}) {
  const [touched, setTouched] = useState([false, false])
  const bothTouched = touched[0] && touched[1]

  const startTime = useFormField(bind, 'startTime')
  const endTime = useFormField(bind, 'endTime')
  const inputInfo = bind.inputInfo()
  return (
    <div>
      <FixedSpaceRow alignItems="center">
        <TimeInputF
          data-qa="reservation-start-time"
          bind={startTime}
          onBlur={() => setTouched(([_, t]) => [true, t])}
          hideErrorsBeforeTouched={!showAllErrors}
        />
        <span>–</span>
        <TimeInputF
          data-qa="reservation-end-time"
          bind={endTime}
          onBlur={() => setTouched(([t, _]) => [t, true])}
          hideErrorsBeforeTouched={!showAllErrors}
        />
      </FixedSpaceRow>
      {inputInfo !== undefined && (showAllErrors || bothTouched) ? (
        <InputFieldUnderRow className={classNames(inputInfo.status)}>
          <span>{inputInfo.text}</span>
          <UnderRowStatusIcon status={inputInfo?.status} />
        </InputFieldUnderRow>
      ) : undefined}
    </div>
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
const DayButtons = styled.div`
  display: flex;
  flex-direction: row;
  gap: ${defaultMargins.s}; // <- counteracts the negative 6px margin icon buttons have
`
