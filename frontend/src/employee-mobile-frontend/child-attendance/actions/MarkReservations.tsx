// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import AttendanceDailyServiceTimes from 'employee-mobile-frontend/child-info/AttendanceDailyServiceTimes'
import { combine } from 'lib-common/api'
import { localTime } from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  union,
  validated,
  value
} from 'lib-common/form/form'
import {
  BoundForm,
  useBoolean,
  useForm,
  useFormElems,
  useFormField,
  useFormFields,
  useFormUnion
} from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { Absence, AbsenceType } from 'lib-common/generated/api-types/absence'
import { ScheduleType } from 'lib-common/generated/api-types/placement'
import {
  ConfirmedRangeDate,
  ConfirmedRangeDateUpdate,
  Reservation
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'
import { groupAbsencesByDateRange } from 'lib-common/utils/absences'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPlus, faTrash, faAngleDown, faAngleUp } from 'lib-icons'

import { routes } from '../../App'
import { renderResult } from '../../async-rendering'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import { Actions, ServiceTime } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import {
  childrenQuery,
  getConfirmedRangeQuery,
  getFutureAbsencesByChildQuery,
  setConfirmedRangeMutation
} from '../queries'
import { useChild } from '../utils'

type Mode = 'view' | 'edit'
const MAX_RESERVATIONS_PER_DAY = 2

const reservationTimeForm = () =>
  validated(
    object({
      startTime: localTime(),
      endTime: localTime()
    }),
    (output) => {
      if (output.startTime === undefined && output.endTime !== undefined) {
        return { startTime: 'required' }
      }
      if (output.startTime !== undefined && output.endTime === undefined) {
        return { endTime: 'required' }
      }
      if (output.startTime !== undefined && output.endTime !== undefined) {
        if (output.startTime.isEqualOrAfter(output.endTime)) {
          return { endTime: 'timeFormat' }
        }
      }
      return undefined
    }
  )

const reservationTimesForm = () => array(reservationTimeForm())

const reservation = () =>
  union({
    times: reservationTimesForm(),
    absence: object({
      absenceType: value<AbsenceType>(),
      scheduleType: value<ScheduleType>()
    }),
    fixedSchedule: value<true>(),
    termBreak: value<true>()
  })

const reservationForm = mapped(
  validated(
    object({
      date: value<LocalDate>(),
      reservation: reservation()
    }),
    (output) => {
      if (output.reservation.branch === 'times') {
        const times = output.reservation.value
        return times.some((time, index, array) =>
          array.some(
            (other, otherIndex) =>
              index !== otherIndex &&
              (time.endTime === undefined ||
                other.startTime === undefined ||
                !time.endTime.isEqualOrBefore(other.startTime)) &&
              (other.endTime === undefined ||
                time.startTime === undefined ||
                !other.endTime.isEqualOrBefore(time.startTime))
          )
        )
          ? 'overlap'
          : undefined
      }
      return undefined
    }
  ),
  (output): ConfirmedRangeDateUpdate | undefined =>
    output.reservation.branch === 'times'
      ? {
          date: output.date,
          reservations: output.reservation.value
            .filter(
              (
                reservation
              ): reservation is { startTime: LocalTime; endTime: LocalTime } =>
                reservation.startTime !== undefined &&
                reservation.endTime !== undefined
            )
            .map((reservation) => ({
              type: 'TIMES',
              range: new TimeRange(reservation.startTime, reservation.endTime)
            })),
          absenceType: null
        }
      : output.reservation.branch === 'absence'
        ? {
            date: output.date,
            reservations: [],
            absenceType: output.reservation.value.absenceType
          }
        : output.reservation.branch === 'fixedSchedule'
          ? {
              date: output.date,
              reservations: [],
              absenceType: null
            }
          : undefined
)

const reservationsForm = object({
  reservations: array(reservationForm)
})

const initialFormState = (
  reservations: ConfirmedRangeDate[]
): StateOf<typeof reservationsForm> => ({
  reservations: sortBy(reservations, (reservation) => reservation.date).map(
    (reservation) => ({
      date: reservation.date,
      reservation:
        reservation.absenceType !== null
          ? {
              branch: 'absence' as const,
              state: {
                absenceType: reservation.absenceType,
                scheduleType: reservation.scheduleType
              }
            }
          : reservation.scheduleType === 'FIXED_SCHEDULE'
            ? { branch: 'fixedSchedule' as const, state: true }
            : reservation.scheduleType === 'TERM_BREAK'
              ? { branch: 'termBreak' as const, state: true }
              : {
                  branch: 'times' as const,
                  state:
                    reservation.reservations.length > 0
                      ? sortBy(
                          reservation.reservations,
                          reservationStartTime
                        ).map((res) =>
                          res.type === 'TIMES'
                            ? {
                                startTime: res.range.formatStart(),
                                endTime: res.range.formatEnd()
                              }
                            : { startTime: '', endTime: '' }
                        )
                      : [{ startTime: '', endTime: '' }]
                }
    })
  )
})

export default React.memo(function MarkReservations({
  unitId,
  childId
}: {
  unitId: UUID
  childId: UUID
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)
  const reservations = useQueryResult(getConfirmedRangeQuery({ childId }))
  const absences = useQueryResult(getFutureAbsencesByChildQuery({ childId }))
  const [mode, setMode] = useState<Mode>('view')
  const goBack = useCallback(() => {
    navigate(-1)
  }, [navigate])

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      {renderResult(child, (child) => (
        <>
          <div>
            <ChildNameBackButton child={child} onClick={goBack} />
          </div>
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="s"
          >
            <Title size={2} primary noMargin>
              {i18n.attendances.actions.markReservations}
            </Title>
            <Gap size="s" />

            {mode === 'view' &&
              renderResult(
                combine(reservations, absences),
                ([reservations, absences]) => (
                  <ReservationsView
                    reservations={reservations}
                    absences={absences}
                    onEditReservations={() => setMode('edit')}
                    onMarkAbsence={() =>
                      navigate(
                        routes.childMarkAbsentBeforehand(unitId, childId).value
                      )
                    }
                  />
                )
              )}
            {mode === 'edit' &&
              renderResult(reservations, (reservations) => (
                <ReservationsEdit
                  childId={childId}
                  reservations={reservations}
                  onCancel={() => setMode('view')}
                  onSuccess={() => setMode('view')}
                />
              ))}
          </ContentArea>
        </>
      ))}
    </TallContentArea>
  )
})

const ReservationsView = ({
  reservations,
  absences,
  onEditReservations,
  onMarkAbsence
}: {
  reservations: ConfirmedRangeDate[]
  absences: Absence[]
  onEditReservations: () => void
  onMarkAbsence: () => void
}) => {
  const { i18n, lang } = useTranslation()
  const { colors } = useTheme()

  const minDatesByWeek = useMemo(
    () => getMinDatesByWeek(reservations),
    [reservations]
  )
  const maxDate = reservations.reduce<LocalDate | null>(
    (maxDate, reservation) =>
      maxDate === null || reservation.date.isAfter(maxDate)
        ? reservation.date
        : maxDate,
    null
  )
  const laterAbsences = useMemo(
    () =>
      maxDate !== null
        ? absences.filter((absence) => absence.date.isAfter(maxDate))
        : absences,
    [absences, maxDate]
  )
  const [laterAbsencesVisible, { toggle: toggleLaterAbsencesVisible }] =
    useBoolean(false)

  return (
    <>
      {reservations.map((reservation) => (
        <React.Fragment key={reservation.date.format()}>
          {minDatesByWeek[reservation.date.getIsoWeek()] ===
            reservation.date && (
            <Label primary>
              {i18n.common.week} {reservation.date.getIsoWeek()}
            </Label>
          )}
          <FixedSpaceRow
            alignItems="baseline"
            data-qa={`reservation-date-${reservation.date.formatIso()}`}
          >
            <Label>{reservation.date.format('EEEEEE d.M.', lang)}</Label>
            <ReservationView reservation={reservation} />
          </FixedSpaceRow>
          <Gap size="s" />
        </React.Fragment>
      ))}
      {laterAbsences.length > 0 ? (
        <>
          <HorizontalLine slim dashed />
          <LaterAbsencesLabel primary onClick={toggleLaterAbsencesVisible}>
            {laterAbsencesVisible
              ? i18n.absences.laterAbsence.open
              : i18n.absences.laterAbsence.closed}
            <FontAwesomeIcon
              icon={laterAbsencesVisible ? faAngleUp : faAngleDown}
              color={colors.main.m2}
            />
          </LaterAbsencesLabel>
          {laterAbsencesVisible ? (
            <AbsenceRanges absences={laterAbsences} />
          ) : null}
        </>
      ) : null}
      <HorizontalLine slim />
      <Actions>
        <FixedSpaceRow
          fullWidth
          flexWrap="wrap"
          spacing={defaultMargins.zero}
          gap={defaultMargins.s}
        >
          <Button
            text={
              i18n.attendances.actions.confirmedRangeReservations
                .markReservations
            }
            onClick={onEditReservations}
            data-qa="edit"
          />
          <Button
            primary
            text={
              i18n.attendances.actions.confirmedRangeReservations
                .markAbsentBeforehand
            }
            onClick={onMarkAbsence}
            data-qa="mark-absent-beforehand"
          />
        </FixedSpaceRow>
      </Actions>
    </>
  )
}

const LaterAbsencesLabel = styled(Label)`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.xs};

  &:hover {
    cursor: pointer;
  }
`

const AbsenceRanges = React.memo(function AbsenceRanges({
  absences
}: {
  absences: Absence[]
}) {
  const absenceRanges = useMemo(
    () => groupAbsencesByDateRange(absences),
    [absences]
  )

  return (
    <>
      {absenceRanges.map((absenceRange) => (
        <div key={absenceRange.start.format()} data-qa="absence-row">
          {absenceRange.durationInDays() > 1
            ? absenceRange.format()
            : absenceRange.start.format()}
        </div>
      ))}
    </>
  )
})

const ReservationView = ({
  reservation
}: {
  reservation: ConfirmedRangeDate
}) => {
  const { i18n } = useTranslation()

  return (
    <div data-qa="reservation-text">
      {reservation.absenceType !== null ? (
        <ServiceTime>{i18n.attendances.types.ABSENT}</ServiceTime>
      ) : (
        <AttendanceDailyServiceTimes
          hideLabel={true}
          dailyServiceTimes={reservation.dailyServiceTimes}
          reservations={sortBy(reservation.reservations, reservationStartTime)}
          scheduleType={reservation.scheduleType}
        />
      )}
    </div>
  )
}

const ReservationsEdit = ({
  childId,
  reservations: data,
  onCancel,
  onSuccess
}: {
  childId: UUID
  reservations: ConfirmedRangeDate[]
  onCancel: () => void
  onSuccess: () => void
}) => {
  const { i18n, lang } = useTranslation()
  const form = useForm(
    reservationsForm,
    () => initialFormState(data),
    i18n.attendances.validationErrors
  )
  const reservations = useFormField(form, 'reservations')
  const reservationElems = useFormElems(reservations)

  const minDatesByWeek = useMemo(
    () => getMinDatesByWeek(reservations.state),
    [reservations]
  )

  return (
    <>
      {reservationElems.map((reservation) => (
        <React.Fragment key={reservation.state.date.format()}>
          {minDatesByWeek[reservation.state.date.getIsoWeek()] ===
            reservation.state.date && (
            <Label primary>
              {i18n.common.week} {reservation.state.date.getIsoWeek()}
            </Label>
          )}
          <FixedSpaceRow
            alignItems="baseline"
            data-qa={`reservation-date-${reservation.state.date.formatIso()}`}
          >
            <Label>{reservation.state.date.format('EEEEEE d.M.', lang)}</Label>
            <ServiceTime>
              <ReservationEdit bind={reservation} />
            </ServiceTime>
          </FixedSpaceRow>
          <Gap size="s" />
        </React.Fragment>
      ))}
      <HorizontalLine slim />
      <Actions>
        <FixedSpaceRow
          fullWidth
          flexWrap="wrap"
          spacing={defaultMargins.zero}
          gap={defaultMargins.s}
        >
          <Button text={i18n.common.cancel} onClick={onCancel} />
          <MutateButton
            primary
            text={i18n.common.confirm}
            mutation={setConfirmedRangeMutation}
            onClick={() => ({
              childId,
              body: reservations
                .value()
                .flatMap((reservation) => reservation ?? [])
            })}
            onSuccess={onSuccess}
            disabled={!form.isValid()}
            data-qa="confirm"
          />
        </FixedSpaceRow>
      </Actions>
    </>
  )
}

const ReservationEdit = ({
  bind
}: {
  bind: BoundForm<typeof reservationForm>
}) => {
  const { i18n } = useTranslation()
  const reservation = useFormField(bind, 'reservation')
  const { branch, form } = useFormUnion(reservation)
  switch (branch) {
    case 'times':
      return <TimesEdit bind={form} />
    case 'absence':
      return (
        <Button
          appearance="link"
          onClick={() => {
            if (form.state.scheduleType === 'FIXED_SCHEDULE') {
              reservation.set({ branch: 'fixedSchedule', state: true })
            } else {
              reservation.set({
                branch: 'times',
                state: [{ startTime: '', endTime: '' }]
              })
            }
          }}
          data-qa="remove-absence"
          text={i18n.attendances.removeAbsence}
        />
      )
    case 'fixedSchedule':
      return (
        <div data-qa="fixed-schedule">
          {i18n.attendances.serviceTime.present}
        </div>
      )
    case 'termBreak':
      return <div data-qa="term-break">{i18n.attendances.termBreak}</div>
  }
}

const TimesEdit = ({
  bind
}: {
  bind: BoundForm<ReturnType<typeof reservationTimesForm>>
}) => {
  const formInputInfo = bind.inputInfo()
  const boundTimes = useFormElems(bind)
  const addButtonLocation =
    bind.isValid() &&
    bind.state.length < MAX_RESERVATIONS_PER_DAY &&
    bind.state.every(
      (reservation) =>
        reservation.startTime !== '' && reservation.endTime !== ''
    )
      ? boundTimes.length === 1
        ? 'same-row'
        : boundTimes.length > 1
          ? 'next-row'
          : undefined
      : undefined

  return (
    <div>
      {boundTimes.map((time, index) => (
        <FixedSpaceRow
          key={index}
          alignItems="center"
          data-qa="reservation-time"
        >
          <ReservationTimeEdit
            form={time}
            onRemoveTime={
              index !== 0
                ? () =>
                    bind.update((prev) => [
                      ...prev.slice(0, index),
                      ...prev.slice(index + 1)
                    ])
                : undefined
            }
          />
          {addButtonLocation === 'same-row' && (
            <ReservationTimeAddButton form={bind} />
          )}
        </FixedSpaceRow>
      ))}
      {addButtonLocation === 'next-row' && (
        <ReservationTimeAddButton form={bind} />
      )}
      {formInputInfo && (
        <InputFieldUnderRow className={classNames(formInputInfo.status)}>
          <span>{formInputInfo.text}</span>
          <UnderRowStatusIcon status={formInputInfo.status} />
        </InputFieldUnderRow>
      )}
    </div>
  )
}

const ReservationTimeAddButton = ({
  form
}: {
  form: BoundForm<ReturnType<typeof reservationTimesForm>>
}) => {
  const { i18n } = useTranslation()

  return (
    <IconOnlyButton
      icon={faPlus}
      onClick={() =>
        form.update((prev) => [...prev, { startTime: '', endTime: '' }])
      }
      aria-label={i18n.common.add}
      data-qa="reservation-time-add"
    />
  )
}

const ReservationTimeEdit = ({
  form,
  onRemoveTime
}: {
  form: BoundForm<ReturnType<typeof reservationTimeForm>>
  onRemoveTime?: () => void
}) => {
  const { i18n } = useTranslation()
  const { startTime, endTime } = useFormFields(form)

  return (
    <>
      <TimeInputF bind={startTime} data-qa="reservation-start-time" />
      <span>–</span>
      <TimeInputF bind={endTime} data-qa="reservation-end-time" />
      {onRemoveTime !== undefined && (
        <IconOnlyButton
          icon={faTrash}
          onClick={onRemoveTime}
          aria-label={i18n.common.remove}
          data-qa="reservation-time-remove"
        />
      )}
    </>
  )
}

const getMinDatesByWeek = (items: { date: LocalDate }[]) =>
  items.reduce<Record<number, LocalDate | undefined>>((weeks, { date }) => {
    const week = date.getIsoWeek()
    const minDate = weeks[week]
    return {
      ...weeks,
      [week]: minDate === undefined || date.isBefore(minDate) ? date : minDate
    }
  }, {})

const reservationStartTime = (reservation: Reservation): LocalTime =>
  reservation.type === 'TIMES'
    ? reservation.range.start.asLocalTime()
    : LocalTime.MIN
