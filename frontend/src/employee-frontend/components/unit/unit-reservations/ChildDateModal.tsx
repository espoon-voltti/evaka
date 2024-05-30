// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, fasExclamationTriangle, faTrash } from 'Icons'
import React, { useState } from 'react'
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
} from 'lib-common/generated/api-types/absence'
import {
  Child,
  ChildDatePresence,
  ChildRecordOfDay,
  ReservationResponse,
  UnitDateInfo
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
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
import {
  childDateExpectedAbsencesQuery,
  upsertChildDatePresenceMutation
} from '../queries'

export interface ChildDateEditorTarget {
  date: LocalDate
  dateInfo: UnitDateInfo
  child: Child
  childDayRecord: ChildRecordOfDay
}

const reservationForm = required(localTimeRange({ allowMidnightEnd: false }))

const attendanceForm = required(openEndedLocalTimeRange())

const absenceForm = oneOf<AbsenceType>()

const absenceErrorCodes = ['attendanceInFuture'] as const
type AbsenceErrorCode = (typeof absenceErrorCodes)[number]
function isAbsenceErrorCode(
  errorCode: string | undefined
): errorCode is AbsenceErrorCode {
  return !!errorCode && absenceErrorCodes.some((e) => e === errorCode)
}

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
      const prev = attendances[i - 1]
      if (prev.overlaps(attendances[i])) {
        return ValidationError.field('attendances', 'timeFormat')
      }
    }

    const result: ChildDatePresence = {
      date,
      childId,
      unitId,
      reservations: reservationNoTimes
        ? [{ type: 'NO_TIMES' }]
        : reservations.map((range) => ({ type: 'TIMES', range })),
      attendances,
      absenceBillable: billableAbsence ?? null,
      absenceNonbillable: nonBillableAbsence ?? null
    }
    return ValidationSuccess.of(result)
  }
)

const excludedAbsenceTypes: AbsenceType[] = [
  'FREE_ABSENCE',
  'PARENTLEAVE',
  'PLANNED_ABSENCE',
  'FORCE_MAJEURE'
]
const basicAbsenceTypes = absenceTypes.filter(
  (t) => !excludedAbsenceTypes.includes(t)
)
const getAbsenceTypeOptions = (
  currentSelection: AbsenceType | null,
  names: Record<AbsenceType, string>
) => {
  const availableTypes = [...basicAbsenceTypes]
  if (currentSelection && !availableTypes.includes(currentSelection)) {
    availableTypes.push(currentSelection)
  }
  return availableTypes.map((at) => ({
    domValue: at,
    value: at,
    label: names[at],
    dataQa: at
  }))
}

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

  const [mutationError, setMutationError] = useState<string | null>(null)

  const boundForm = useForm(
    form,
    () => ({
      date: localDate.fromDate(date),
      childId: child.id,
      unitId,
      reservations:
        childDayRecord.scheduleType === 'RESERVATION_REQUIRED'
          ? childDayRecord.reservations
              .filter((r): r is ReservationResponse.Times => r.type === 'TIMES')
              .map((r) => ({
                startTime: r.range.formatStart(),
                endTime: r.range.formatEnd()
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
            startTime: a.formatStart(),
            endTime: a.formatEnd()
          })),
      billableAbsence: childDayRecord.possibleAbsenceCategories.includes(
        'BILLABLE'
      )
        ? {
            domValue: childDayRecord.absenceBillable
              ? childDayRecord.absenceBillable.absenceType
              : '',
            options: getAbsenceTypeOptions(
              childDayRecord.absenceBillable?.absenceType ?? null,
              i18n.absences.absenceTypes
            )
          }
        : {
            domValue: '',
            options: []
          },
      nonBillableAbsence: childDayRecord.possibleAbsenceCategories.includes(
        'NONBILLABLE'
      )
        ? {
            domValue: childDayRecord.absenceNonbillable
              ? childDayRecord.absenceNonbillable.absenceType
              : '',
            options: getAbsenceTypeOptions(
              childDayRecord.absenceNonbillable?.absenceType ?? null,
              i18n.absences.absenceTypes
            )
          }
        : {
            domValue: '',
            options: []
          }
    }),
    i18n.validationErrors,
    {
      onUpdate: (_, nextState) => {
        setMutationError(null)
        return nextState
      }
    }
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

  const expectedAbsences = useQueryResult(
    queryOrDefault(
      (attendances: TimeRange[]) =>
        childDateExpectedAbsencesQuery({
          body: {
            childId: child.id,
            date,
            attendances
          }
        }),
      null
    )(
      date.isBefore(LocalDate.todayInHelsinkiTz()) &&
        attendances.isValid() &&
        attendances.value().every((a) => a.end !== null)
        ? attendances.value().flatMap((a) => a.asTimeRange()!)
        : null
    )
  )

  return (
    <MutateFormModal
      title={date.formatExotic('EEEEEE d.M.yyyy')}
      resolveMutation={upsertChildDatePresenceMutation}
      resolveDisabled={!boundForm.isValid()}
      resolveAction={() => ({ body: boundForm.value() })}
      resolveLabel={i18n.common.save}
      onSuccess={onClose}
      onFailure={(e) => {
        if (isAbsenceErrorCode(e.errorCode)) {
          setMutationError(
            i18n.unit.attendanceReservations.childDateModal.errorCodes[
              e.errorCode
            ]
          )
        }
      }}
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
                data-qa={`reservation-${i}`}
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
                data-qa="add-reservation"
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
                data-qa={`attendance-${i}`}
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
              data-qa="add-attendance"
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
            bind={nonBillableAbsence}
            category="NONBILLABLE"
            otherAbsence={billableAbsence.value()}
          />
        )}
        {childDayRecord.possibleAbsenceCategories.includes('BILLABLE') && (
          <AbsenceForm
            bind={billableAbsence}
            category="BILLABLE"
            otherAbsence={nonBillableAbsence.value()}
          />
        )}
        {expectedAbsences.isSuccess && expectedAbsences.value !== null && (
          <FixedSpaceColumn data-qa="absence-warnings">
            {expectedAbsences.value.includes('NONBILLABLE') &&
              nonBillableAbsence.value() === undefined && (
                <AbsenceWarning
                  data-qa="missing-nonbillable-absence"
                  message={
                    i18n.unit.attendanceReservations.childDateModal
                      .missingNonbillableAbsence
                  }
                />
              )}
            {!expectedAbsences.value.includes('NONBILLABLE') &&
              nonBillableAbsence.value() !== undefined && (
                <AbsenceWarning
                  data-qa="extra-nonbillable-absence"
                  message={
                    i18n.unit.attendanceReservations.childDateModal
                      .extraNonbillableAbsence
                  }
                />
              )}
            {expectedAbsences.value.includes('BILLABLE') &&
              billableAbsence.value() === undefined && (
                <AbsenceWarning
                  data-qa="missing-billable-absence"
                  message={
                    i18n.unit.attendanceReservations.childDateModal
                      .missingBillableAbsence
                  }
                />
              )}
            {!expectedAbsences.value.includes('BILLABLE') &&
              billableAbsence.value() !== undefined && (
                <AbsenceWarning
                  data-qa="extra-billable-absence"
                  message={
                    i18n.unit.attendanceReservations.childDateModal
                      .extraBillableAbsence
                  }
                />
              )}
          </FixedSpaceColumn>
        )}
      </FixedSpaceColumn>
      {!!mutationError && <AlertBox message={mutationError} />}
    </MutateFormModal>
  )
})

const TimesForm = React.memo(function TimesForm({
  bind,
  onRemove,
  'data-qa': dataQa
}: {
  bind: BoundForm<typeof reservationForm | typeof attendanceForm>
  onRemove: () => void
  'data-qa': string
}) {
  const { i18n } = useTranslation()
  const { startTime, endTime } = useFormFields(bind)
  return (
    <FixedSpaceRow alignItems="center" data-qa={dataQa}>
      <TimeInputF bind={startTime} data-qa="start" />
      <span>-</span>
      <TimeInputF bind={endTime} data-qa="end" />
      <IconButton
        icon={faTrash}
        aria-label={i18n.common.remove}
        data-qa="remove-btn"
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
        data-qa={`add-${category.toLowerCase()}-absence`}
      />
    )
  }

  return (
    <FixedSpaceRow
      alignItems="center"
      data-qa={`${category.toLowerCase()}-absence`}
    >
      <AbsenceLabel>
        {
          i18n.unit.attendanceReservations.childDateModal.absences.label[
            category
          ]
        }
      </AbsenceLabel>
      <SelectF bind={bind} data-qa="type-select" />
      <IconButton
        data-qa="remove-btn"
        icon={faTrash}
        aria-label={i18n.common.remove}
        onClick={() => bind.update((s) => ({ ...s, domValue: '' }))}
      />
    </FixedSpaceRow>
  )
})

const AbsenceWarning = React.memo(function AbsenceWarning({
  message,
  ['data-qa']: dataQa
}: {
  message: string
  'data-qa': string
}) {
  const { i18n } = useTranslation()
  return (
    <div>
      <AlertBox
        data-qa={dataQa}
        title={i18n.unit.attendanceReservations.childDateModal.absenceWarning}
        message={message}
        thin
        noMargin
      />
    </div>
  )
})

const AbsenceLabel = styled.div`
  width: 135px;
`
