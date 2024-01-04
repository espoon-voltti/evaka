import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, fasExclamationTriangle, faTrash } from 'Icons'
import React from 'react'

import {
  boolean,
  localDate,
  localTimeRange,
  openEndedLocalTimeRange,
  string
} from 'lib-common/form/fields'
import { array, object, required, transformed } from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import {
  Child,
  ChildDatePresence,
  ChildRecordOfDay,
  Reservation,
  UnitDateInfo
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H3, H4 } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'
import { upsertChildDatePresenceMutation } from '../queries'

export interface ChildDateEditorTarget {
  date: LocalDate
  dateInfo: UnitDateInfo
  child: Child
  childDayRecord: ChildRecordOfDay
}

const reservationForm = required(localTimeRange())

const attendanceForm = required(openEndedLocalTimeRange())

const form = transformed(
  object({
    date: required(localDate()),
    childId: required(string()),
    unitId: required(string()),
    reservations: array(reservationForm),
    reservationNoTimes: boolean(),
    attendances: array(attendanceForm)
  }),
  ({
    date,
    childId,
    unitId,
    reservations,
    reservationNoTimes,
    attendances
  }) => {
    for (let i = 1; i < reservations.length; i++) {
      if (reservations[i].start.isBefore(reservations[i - 1].end)) {
        return ValidationError.field('reservations', 'timeFormat')
      }
    }
    for (let i = 1; i < attendances.length; i++) {
      const prevEnd = attendances[i - 1].endTime
      if (!prevEnd || attendances[i].startTime.isBefore(prevEnd)) {
        return ValidationError.field('attendances', 'timeFormat')
      }
    }

    const result: ChildDatePresence = {
      date,
      childId,
      unitId,
      reservations: reservationNoTimes
        ? [{ type: 'NO_TIMES' }]
        : reservations.map((r) => ({
            type: 'TIMES',
            startTime: r.start,
            endTime: r.end
          })),
      attendances,
      absences: []
    }
    return ValidationSuccess.of(result)
  }
)

export default React.memo(function ChildDateModal({
  target: { date, dateInfo, child, childDayRecord },
  unitId,
  onClose
}: {
  target: ChildDateEditorTarget
  unitId: UUID
  onClose: () => void
}) {
  const { i18n } = useTranslation()

  const boundForm = useForm(
    form,
    () => ({
      date: localDate.fromDate(date),
      childId: child.id,
      unitId,
      reservations: childDayRecord.reservations
        .filter((r): r is Reservation.Times => r.type === 'TIMES')
        .map((r) => ({
          startTime: r.startTime.format(),
          endTime: r.endTime.format()
        })),
      reservationNoTimes:
        dateInfo.isInHolidayPeriod &&
        childDayRecord.reservations.length === 1 &&
        childDayRecord.reservations[0].type === 'NO_TIMES',
      attendances: childDayRecord.attendances.map((a) => ({
        startTime: a.startTime.format(),
        endTime: a.endTime?.format() ?? ''
      }))
    }),
    i18n.validationErrors
  )

  const { reservations, reservationNoTimes, attendances } =
    useFormFields(boundForm)
  const reservationElems = useFormElems(reservations)
  const attendanceElems = useFormElems(attendances)

  return (
    <MutateFormModal
      title={date.formatExotic('EEEEEE d.M.yyyy')}
      resolveMutation={upsertChildDatePresenceMutation}
      resolveDisabled={!boundForm.isValid()}
      resolveAction={() => boundForm.value()}
      resolveLabel={i18n.common.save}
      onSuccess={onClose}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
    >
      <H3>
        {child.preferredName || child.firstName} {child.lastName}
      </H3>

      <H4>Läsnäolovaraus</H4>
      <FixedSpaceColumn>
        {reservationElems.map((r, i) => (
          <TimesForm
            key={`reservation-${i}`}
            bind={r}
            onRemove={() =>
              reservations.update((s) => [...s.slice(0, i), ...s.slice(i + 1)])
            }
          />
        ))}
        {reservationElems.length < 2 &&
          reservationNoTimes.value() === false && (
            <InlineButton
              text="Lisää varaus"
              icon={faPlus}
              onClick={() =>
                reservations.update((s) => [
                  ...s,
                  { startTime: '', endTime: '' }
                ])
              }
            />
          )}
        {reservationElems.length === 0 && dateInfo.isInHolidayPeriod && (
          <CheckboxF
            bind={reservationNoTimes}
            label="Läsnä, kellonaika ei vielä tiedossa"
          />
        )}
        {!reservations.isValid() &&
          reservations.validationError() === 'timeFormat' && (
            <AlertBox message="Tarkista päällekkäisyys" thin noMargin />
          )}
      </FixedSpaceColumn>

      <H4>Läsnäolototeuma</H4>
      <FixedSpaceColumn>
        {attendanceElems.map((a, i) => (
          <TimesForm
            key={`attendance-${i}`}
            bind={a}
            onRemove={() =>
              attendances.update((s) => [...s.slice(0, i), ...s.slice(i + 1)])
            }
          />
        ))}
        <InlineButton
          text="Lisää uusi rivi"
          icon={faPlus}
          onClick={() =>
            attendances.update((s) => [...s, { startTime: '', endTime: '' }])
          }
        />
        {!attendances.isValid() &&
          attendances.validationError() === 'timeFormat' && (
            <AlertBox message="Tarkista päällekkäisyys" thin noMargin />
          )}
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})

const TimesForm = React.memo(function ReservationTimesForm({
  bind,
  onRemove
}: {
  bind: BoundForm<typeof reservationForm | typeof attendanceForm>
  onRemove: () => void
}) {
  const { i18n } = useTranslation()
  const { startTime, endTime } = useFormFields(bind)
  return (
    <FixedSpaceRow alignItems="center">
      <TimeInputF bind={startTime} />
      <span>-</span>
      <TimeInputF bind={endTime} />
      <IconButton
        icon={faTrash}
        aria-label={i18n.common.remove}
        onClick={onRemove}
      />
      {startTime.isValid() && endTime.isValid() && !bind.isValid() && (
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.status.warning}
        />
      )}
    </FixedSpaceRow>
  )
})
