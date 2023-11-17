// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import AttendanceDailyServiceTimes from 'employee-mobile-frontend/child-info/AttendanceDailyServiceTimes'
import { useSelectedGroup } from 'employee-mobile-frontend/common/selected-group'
import { combine } from 'lib-common/api'
import { localTime } from 'lib-common/form/fields'
import { array, mapped, object, validated, value } from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { Absence, AbsenceType } from 'lib-common/generated/api-types/daycare'
import {
  NonReservableReservation,
  Reservation
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { groupAbsencesByDateRange } from 'lib-common/utils/absences'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import Title from 'lib-components/atoms/Title'
import Button from 'lib-components/atoms/buttons/Button'
import { ButtonLink } from 'lib-components/atoms/buttons/ButtonLink'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPlus, faTrash } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import { Actions, ServiceTime } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import {
  childrenQuery,
  getFutureAbsencesByChildQuery,
  getNonReservableReservationsQuery,
  setNonReservableReservationsMutation
} from '../queries'
import { useChild } from '../utils'

type Mode = 'view' | 'edit'
const MAX_RESERVATIONS_PER_DAY = 2

const reservationTimeForm = validated(
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

const reservationTimesForm = array(reservationTimeForm)

const reservationForm = mapped(
  validated(
    object({
      date: value<LocalDate>(),
      times: reservationTimesForm,
      absenceType: value<AbsenceType | null>()
    }),
    (output) => {
      if (
        output.times.some((time, index, array) =>
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
      ) {
        return 'overlap'
      }
      return undefined
    }
  ),
  (output): NonReservableReservation => ({
    date: output.date,
    reservations: output.times
      .filter(
        (
          reservation
        ): reservation is { startTime: LocalTime; endTime: LocalTime } =>
          reservation.startTime !== undefined &&
          reservation.endTime !== undefined
      )
      .map((reservation) => ({
        ...reservation,
        type: 'TIMES'
      })),
    absenceType: output.absenceType,
    dailyServiceTimes: null
  })
)

const reservationsForm = object({
  reservations: array(reservationForm)
})

const initialFormState = (
  reservations: NonReservableReservation[]
): StateOf<typeof reservationsForm> => ({
  reservations: sortBy(reservations, (reservation) => reservation.date).map(
    (reservation) => ({
      date: reservation.date,
      times:
        reservation.reservations.length > 0
          ? sortBy(reservation.reservations, reservationStartTime).map((res) =>
              res.type === 'TIMES'
                ? {
                    startTime: res.startTime.format(),
                    endTime: res.endTime.format()
                  }
                : { startTime: '', endTime: '' }
            )
          : [{ startTime: '', endTime: '' }],
      absenceType: reservation.absenceType
    })
  )
})

export default React.memo(function MarkReservations() {
  const navigate = useNavigate()
  const { groupRoute } = useSelectedGroup()
  const { i18n } = useTranslation()
  const { childId, unitId } = useNonNullableParams<{
    unitId: string
    childId: string
  }>()
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)
  const reservations = useQueryResult(
    getNonReservableReservationsQuery(childId)
  )
  const absences = useQueryResult(getFutureAbsencesByChildQuery(childId))
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
                        `${groupRoute}/child-attendance/${childId}/mark-absent-beforehand`
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
  reservations: NonReservableReservation[]
  absences: Absence[]
  onEditReservations: () => void
  onMarkAbsence: () => void
}) => {
  const { i18n, lang } = useTranslation()

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
  const laterAbsences =
    maxDate !== null
      ? absences.filter((absence) => absence.date.isAfter(maxDate))
      : absences

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
          <Label primary>{i18n.absences.laterAbsence}</Label>
          {groupAbsencesByDateRange(laterAbsences).map((absenceRange) => {
            if (absenceRange.durationInDays() > 1) {
              return (
                <div key={absenceRange.start.format()} data-qa="absence-row">
                  {`${absenceRange.start.format()} - ${absenceRange.end.format()}`}
                </div>
              )
            } else {
              return (
                <div key={absenceRange.start.format()} data-qa="absence-row">
                  {absenceRange.start.format()}
                </div>
              )
            }
          })}
        </>
      ) : null}
      <HorizontalLine slim />
      <Actions>
        <FixedSpaceRow fullWidth>
          <Button
            text={
              i18n.attendances.actions.nonReservableReservations
                .markReservations
            }
            onClick={onEditReservations}
            data-qa="edit"
          />
          <Button
            primary
            text={
              i18n.attendances.actions.nonReservableReservations
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

const ReservationView = ({
  reservation
}: {
  reservation: NonReservableReservation
}) => {
  const { i18n } = useTranslation()

  return (
    <div data-qa="reservation-text">
      {reservation.absenceType !== null ? (
        <ServiceTime>{i18n.attendances.types.ABSENT}</ServiceTime>
      ) : (
        <AttendanceDailyServiceTimes
          hideLabel={true}
          times={reservation.dailyServiceTimes}
          reservations={sortBy(reservation.reservations, reservationStartTime)}
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
  reservations: NonReservableReservation[]
  onCancel: () => void
  onSuccess: () => void
}) => {
  const { i18n, lang } = useTranslation()
  const form = useForm(
    reservationsForm,
    () => initialFormState(data),
    i18n.attendances.validationErrors
  )
  const { reservations } = useFormFields(form)
  const boundReservations = useFormElems(reservations)

  const minDatesByWeek = useMemo(
    () => getMinDatesByWeek(reservations.state),
    [reservations]
  )

  return (
    <>
      {boundReservations.map((reservation) => (
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
            <ReservationEdit form={reservation} />
          </FixedSpaceRow>
          <Gap size="s" />
        </React.Fragment>
      ))}
      <HorizontalLine slim />
      <Actions>
        <FixedSpaceRow fullWidth>
          <Button text={i18n.common.cancel} onClick={onCancel} />
          <MutateButton
            primary
            text={i18n.common.confirm}
            mutation={setNonReservableReservationsMutation}
            onClick={() => ({
              childId,
              body: reservations.value()
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
  form
}: {
  form: BoundForm<typeof reservationForm>
}) => {
  const { times, absenceType } = useFormFields(form)
  const boundTimes = useFormElems(times)

  const formInputInfo = form.inputInfo()
  const addButtonLocation =
    form.isValid() &&
    form.state.times.length < MAX_RESERVATIONS_PER_DAY &&
    form.state.times.every(
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
                    times.update((prev) => [
                      ...prev.slice(0, index),
                      ...prev.slice(index + 1)
                    ])
                : undefined
            }
            onRemoveAbsence={
              absenceType.value() !== null
                ? () => absenceType.update(() => null)
                : undefined
            }
          />
          {addButtonLocation === 'same-row' && (
            <ReservationTimeAddButton form={times} />
          )}
        </FixedSpaceRow>
      ))}
      {addButtonLocation === 'next-row' && (
        <ReservationTimeAddButton form={times} />
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
  form: BoundForm<typeof reservationTimesForm>
}) => {
  const { i18n } = useTranslation()

  return (
    <IconButton
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
  onRemoveTime,
  onRemoveAbsence
}: {
  form: BoundForm<typeof reservationTimeForm>
  onRemoveTime?: () => void
  onRemoveAbsence?: () => void
}) => {
  const { i18n } = useTranslation()
  const { startTime, endTime } = useFormFields(form)

  if (onRemoveAbsence !== undefined) {
    return (
      <ButtonLink onClick={onRemoveAbsence} data-qa="remove-absence">
        {i18n.attendances.removeAbsence}
      </ButtonLink>
    )
  }
  return (
    <>
      <TimeInputF bind={startTime} data-qa="reservation-start-time" />
      <span>–</span>
      <TimeInputF bind={endTime} data-qa="reservation-end-time" />
      {onRemoveTime !== undefined && (
        <IconButton
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
  reservation.type === 'TIMES' ? reservation.startTime : LocalTime.MIN
