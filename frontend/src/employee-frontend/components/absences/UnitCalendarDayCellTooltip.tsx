// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import type {
  AbsenceCategory,
  AbsenceType,
  AbsenceWithModifierInfo,
  ChildReservation
} from 'lib-common/generated/api-types/absence'
import type { ServiceTimesPresenceStatus } from 'lib-common/generated/api-types/dailyservicetimes'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'

export interface AbsenceTooltipItem {
  category: AbsenceCategory
  absenceType: AbsenceType
  modifiedAt: HelsinkiDateTime
  modifiedByStaff: boolean
  modifiedByName: string
}

export const AbsencesTooltipContent = React.memo(
  function AbsencesTooltipContent({
    absences
  }: {
    absences: AbsenceTooltipItem[]
  }) {
    const { i18n } = useTranslation()
    return (
      <FixedSpaceColumn $spacing="xs">
        {absences.map(
          (
            {
              category,
              absenceType,
              modifiedAt,
              modifiedByStaff,
              modifiedByName
            },
            index
          ) => (
            <div key={index}>
              <div>
                {`${i18n.absences.absenceCategories[category]}: ${i18n.absences.absenceTypes[absenceType]}`}
              </div>
              <div>
                {`${modifiedAt.format()} ${
                  modifiedByStaff
                    ? i18n.absences.modifiedByStaff
                    : i18n.absences.modifiedByCitizen(modifiedByName)
                }`}
              </div>
            </div>
          )
        )}
      </FixedSpaceColumn>
    )
  }
)

interface UnitCalendarMonthlyDayCellTooltipProps {
  date: LocalDate
  absences: AbsenceWithModifierInfo[]
  dailyServiceTimes: ServiceTimesPresenceStatus
  reservations: ChildReservation[]
  backupCare: boolean
  isMissingHolidayReservation: boolean
  isMissingQuestionnaireAnswer: boolean
  requiresBackupCare: boolean
}

export default React.memo(function UnitCalendarMonthlyDayCellTooltip({
  date,
  absences,
  dailyServiceTimes,
  reservations,
  backupCare,
  isMissingHolidayReservation,
  isMissingQuestionnaireAnswer,
  requiresBackupCare
}: UnitCalendarMonthlyDayCellTooltipProps) {
  const { i18n } = useTranslation()

  const dailyServiceTimeTooltip = useMemo(
    () =>
      dailyServiceTimes.type === 'PRESENT' ? (
        <div>
          {`${i18n.absences.dailyServiceTime} ${dailyServiceTimes.times.format()}`}
        </div>
      ) : undefined,
    [i18n, dailyServiceTimes]
  )

  const reservationTooltip = useMemo(
    () =>
      reservations.map((res, index) => {
        const reservationText =
          res.reservation.type === 'TIMES'
            ? `${i18n.absences.reservation} ${res.reservation.range.format()}`
            : i18n.absences.present
        const userTypeText =
          res.createdByEvakaUserType === 'CITIZEN'
            ? i18n.absences.modifiedByCitizen(res.createdByName)
            : i18n.absences.modifiedByStaff
        return (
          <div key={index}>
            <div>{reservationText}</div>
            <div>
              {res.created.toLocalDate().format()} {userTypeText}
            </div>
          </div>
        )
      }),
    [i18n, reservations]
  )

  const missingHolidayReservationTooltip = useMemo(
    () => (
      <FixedSpaceColumn $spacing="xs">
        <div>{i18n.absences.missingHolidayReservation}</div>
        {dailyServiceTimeTooltip}
      </FixedSpaceColumn>
    ),
    [i18n, dailyServiceTimeTooltip]
  )

  const missingQuestionnaireAnswerTooltip = useMemo(
    () => (
      <FixedSpaceColumn $spacing="xs">
        <div>{i18n.absences.missingHolidayQuestionnaireAnswer}</div>
        {dailyServiceTimeTooltip}
      </FixedSpaceColumn>
    ),
    [i18n, dailyServiceTimeTooltip]
  )

  const backupCareTooltip = (
    <div>{i18n.absences.absenceTypes.TEMPORARY_RELOCATION}</div>
  )

  const absencesTooltip = useMemo(
    () => <AbsencesTooltipContent absences={absences} />,
    [absences]
  )

  const requiresBackupCareTooltip = useMemo(
    () => (
      <div>
        <div>{i18n.absences.shiftCare}</div>
        <div>{i18n.absences.requiresBackupCare}</div>
      </div>
    ),
    [i18n]
  )

  return (
    <div data-qa={`attendance-tooltip-${date.toString()}`}>
      {backupCare ? (
        backupCareTooltip
      ) : absences.length > 0 ? (
        absencesTooltip
      ) : isMissingHolidayReservation ? (
        missingHolidayReservationTooltip
      ) : featureFlags.missingQuestionnaireAnswerMarkerEnabled &&
        isMissingQuestionnaireAnswer ? (
        missingQuestionnaireAnswerTooltip
      ) : requiresBackupCare ? (
        requiresBackupCareTooltip
      ) : reservations.length > 0 || dailyServiceTimes !== null ? (
        <FixedSpaceColumn $spacing="xs">
          {reservationTooltip}
          {dailyServiceTimeTooltip}
        </FixedSpaceColumn>
      ) : undefined}
    </div>
  )
})
