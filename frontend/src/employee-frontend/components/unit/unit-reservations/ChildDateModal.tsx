import { faPlus, faTrash } from 'Icons'
import React from 'react'

import {
  boolean,
  localDate,
  localTimeRange,
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
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H3, H4 } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'
import { upsertChildDatePresenceMutation } from '../queries'

export interface ChildDateEditorTarget {
  date: LocalDate
  dateInfo: UnitDateInfo
  child: Child
  childDayRecord: ChildRecordOfDay
}

const reservationForm = required(localTimeRange())

const form = transformed(
  object({
    date: required(localDate()),
    childId: required(string()),
    reservations: array(reservationForm),
    reservationNoTimes: boolean()
  }),
  ({ date, childId, reservations, reservationNoTimes }) => {
    if (reservations.length > 0 && reservationNoTimes) {
      return ValidationError.of('unexpectedError')
    }

    const result: ChildDatePresence = {
      date,
      childId,
      reservations: reservationNoTimes
        ? [{ type: 'NO_TIMES' }]
        : reservations.map((r) => ({
            type: 'TIMES',
            startTime: r.start,
            endTime: r.end
          })),
      attendances: [],
      absences: []
    }
    return ValidationSuccess.of(result)
  }
)

export default React.memo(function ChildDateModal({
  target: { date, dateInfo, child, childDayRecord },
  onClose
}: {
  target: ChildDateEditorTarget
  onClose: () => void
}) {
  const { i18n } = useTranslation()

  const boundForm = useForm(
    form,
    () => ({
      date: localDate.fromDate(date),
      childId: child.id,
      reservations: childDayRecord.reservations
        .filter((r): r is Reservation.Times => r.type === 'TIMES')
        .map((r) => ({
          startTime: r.startTime.format(),
          endTime: r.endTime.format()
        })),
      reservationNoTimes:
        dateInfo.isInHolidayPeriod &&
        childDayRecord.reservations.length === 1 &&
        childDayRecord.reservations[0].type === 'NO_TIMES'
    }),
    i18n.validationErrors
  )

  const { reservations, reservationNoTimes } = useFormFields(boundForm)
  const reservationElems = useFormElems(reservations)

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

      <H4>Varaukset</H4>
      <FixedSpaceColumn>
        {reservationElems.map((r, i) => (
          <ReservationTimesForm
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
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})

const ReservationTimesForm = React.memo(function ReservationTimesForm({
  bind,
  onRemove
}: {
  bind: BoundForm<typeof reservationForm>
  onRemove: () => void
}) {
  const { i18n } = useTranslation()
  const { startTime, endTime } = useFormFields(bind)
  return (
    <FixedSpaceRow>
      <TimeInputF bind={startTime} />
      <span>-</span>
      <TimeInputF bind={endTime} />
      <IconButton
        icon={faTrash}
        aria-label={i18n.common.remove}
        onClick={onRemove}
      />
    </FixedSpaceRow>
  )
})
