import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, fasExclamationTriangle, faTrash } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import {
  boolean,
  localDate,
  localTimeRange,
  openEndedLocalTimeRange,
  string
} from 'lib-common/form/fields'
import {
  array,
  object,
  oneOf,
  required,
  transformed
} from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
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
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
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
import { absenceTypes } from 'lib-customizations/employee'

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

const absenceForm = oneOf<AbsenceType>()

const form = transformed(
  object({
    date: required(localDate()),
    childId: required(string()),
    unitId: required(string()),
    reservations: array(reservationForm),
    reservationNoTimes: boolean(),
    attendances: array(attendanceForm),
    billableAbsence: absenceForm,
    nonBillableAbsence: absenceForm
  }),
  ({
    date,
    childId,
    unitId,
    reservations,
    reservationNoTimes,
    attendances,
    billableAbsence,
    nonBillableAbsence
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
      absences: {
        BILLABLE: billableAbsence ?? null,
        NONBILLABLE: nonBillableAbsence ?? null
      }
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

  const today = LocalDate.todayInHelsinkiTz()
  const editingFuture = date.isAfter(today)

  const absenceOptions = absenceTypes.map((at) => ({
    domValue: at,
    value: at,
    label: i18n.absences.absenceTypes[at],
    dataQa: at
  }))

  const boundForm = useForm(
    form,
    () => ({
      date: localDate.fromDate(date),
      childId: child.id,
      unitId,
      reservations:
        childDayRecord.scheduleType === 'RESERVATION_REQUIRED'
          ? childDayRecord.reservations
              .filter((r): r is Reservation.Times => r.type === 'TIMES')
              .map((r) => ({
                startTime: r.startTime.format(),
                endTime: r.endTime.format()
              }))
          : [],
      reservationNoTimes:
        dateInfo.isInHolidayPeriod &&
        childDayRecord.scheduleType === 'RESERVATION_REQUIRED' &&
        childDayRecord.reservations.length === 1 &&
        childDayRecord.reservations[0].type === 'NO_TIMES',
      attendances: editingFuture
        ? []
        : childDayRecord.attendances.map((a) => ({
            startTime: a.startTime.format(),
            endTime: a.endTime?.format() ?? ''
          })),
      billableAbsence: childDayRecord.possibleAbsenceCategories.includes(
        'BILLABLE'
      )
        ? {
            domValue: childDayRecord.absences.BILLABLE ?? '',
            options: absenceOptions
          }
        : {
            domValue: '',
            options: []
          },
      nonBillableAbsence: childDayRecord.possibleAbsenceCategories.includes(
        'NONBILLABLE'
      )
        ? {
            domValue: childDayRecord.absences.NONBILLABLE ?? '',
            options: absenceOptions
          }
        : {
            domValue: '',
            options: []
          }
    }),
    i18n.validationErrors
  )

  const {
    reservations,
    reservationNoTimes,
    attendances,
    billableAbsence,
    nonBillableAbsence
  } = useFormFields(boundForm)
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

      {childDayRecord.scheduleType === 'RESERVATION_REQUIRED' && (
        <>
          <H4>
            {i18n.unit.attendanceReservations.childDateModal.reservations.title}
          </H4>
          <FixedSpaceColumn>
            {reservationElems.map((r, i) => (
              <TimesForm
                key={`reservation-${i}`}
                bind={r}
                onRemove={() =>
                  reservations.update((s) => [
                    ...s.slice(0, i),
                    ...s.slice(i + 1)
                  ])
                }
              />
            ))}
            {reservationElems.length < 2 && (
              <InlineButton
                text={
                  i18n.unit.attendanceReservations.childDateModal.reservations
                    .add
                }
                icon={faPlus}
                disabled={reservationNoTimes.value()}
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
                label={
                  i18n.unit.attendanceReservations.childDateModal.reservations
                    .noTimes
                }
              />
            )}
            {!reservations.isValid() &&
              reservations.validationError() === 'timeFormat' && (
                <AlertBox
                  message={
                    i18n.unit.attendanceReservations.childDateModal
                      .overlapWarning
                  }
                  thin
                  noMargin
                />
              )}
          </FixedSpaceColumn>
        </>
      )}

      {!editingFuture && (
        <>
          <H4>
            {i18n.unit.attendanceReservations.childDateModal.attendances.title}
          </H4>
          <FixedSpaceColumn>
            {attendanceElems.map((a, i) => (
              <TimesForm
                key={`attendance-${i}`}
                bind={a}
                onRemove={() =>
                  attendances.update((s) => [
                    ...s.slice(0, i),
                    ...s.slice(i + 1)
                  ])
                }
              />
            ))}
            <InlineButton
              text={
                i18n.unit.attendanceReservations.childDateModal.attendances.add
              }
              icon={faPlus}
              onClick={() =>
                attendances.update((s) => [
                  ...s,
                  { startTime: '', endTime: '' }
                ])
              }
            />
            {!attendances.isValid() &&
              attendances.validationError() === 'timeFormat' && (
                <AlertBox
                  message={
                    i18n.unit.attendanceReservations.childDateModal
                      .overlapWarning
                  }
                  thin
                  noMargin
                />
              )}
          </FixedSpaceColumn>
        </>
      )}

      <H4>{i18n.unit.attendanceReservations.childDateModal.absences.title}</H4>
      <FixedSpaceColumn>
        {childDayRecord.possibleAbsenceCategories.includes('NONBILLABLE') && (
          <AbsenceForm
            bind={billableAbsence}
            category="NONBILLABLE"
            otherAbsence={nonBillableAbsence.value()}
          />
        )}
        {childDayRecord.possibleAbsenceCategories.includes('BILLABLE') && (
          <AbsenceForm
            bind={nonBillableAbsence}
            category="BILLABLE"
            otherAbsence={billableAbsence.value()}
          />
        )}
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})

const TimesForm = React.memo(function TimesForm({
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

const AbsenceForm = React.memo(function AbsenceForm({
  bind,
  category,
  otherAbsence
}: {
  bind: BoundForm<typeof absenceForm>
  category: AbsenceCategory
  otherAbsence: AbsenceType | undefined
}) {
  const { i18n } = useTranslation()

  const defaultType: AbsenceType = otherAbsence ?? 'OTHER_ABSENCE'

  if (bind.value() === undefined) {
    return (
      <InlineButton
        onClick={() => bind.update((s) => ({ ...s, domValue: defaultType }))}
        text={
          i18n.unit.attendanceReservations.childDateModal.absences.add[category]
        }
        icon={faPlus}
      />
    )
  }

  return (
    <FixedSpaceRow alignItems="center">
      <AbsenceLabel>
        {
          i18n.unit.attendanceReservations.childDateModal.absences.label[
            category
          ]
        }
      </AbsenceLabel>
      <SelectF bind={bind} />
      <IconButton
        icon={faTrash}
        aria-label={i18n.common.remove}
        onClick={() => bind.update((s) => ({ ...s, domValue: '' }))}
      />
    </FixedSpaceRow>
  )
})

const AbsenceLabel = styled.div`
  width: 135px;
`
