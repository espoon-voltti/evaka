// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React from 'react'

import type { EvakaUser } from 'lib-common/generated/api-types/user'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type LocalDate from 'lib-common/local-date'
import { Italic, P } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'

export interface MarkingMetadata {
  addedAt: HelsinkiDateTime | null
  addedBy: EvakaUser | null
  modifiedAt: HelsinkiDateTime | null
  modifiedBy: EvakaUser | null
}

interface AttendanceMarkings {
  arrivedAddedAt: HelsinkiDateTime | null
  arrivedAddedBy: EvakaUser | null
  arrivedModifiedAt: HelsinkiDateTime | null
  arrivedModifiedBy: EvakaUser | null
  departedAddedAt: HelsinkiDateTime | null
  departedAddedBy: EvakaUser | null
  departedModifiedAt: HelsinkiDateTime | null
  departedModifiedBy: EvakaUser | null
}

export const arrivedMetadata = (
  attendance: AttendanceMarkings
): MarkingMetadata => ({
  addedAt: attendance.arrivedAddedAt,
  addedBy: attendance.arrivedAddedBy,
  modifiedAt: attendance.arrivedModifiedAt,
  modifiedBy: attendance.arrivedModifiedBy
})

export const departedMetadata = (
  attendance: AttendanceMarkings
): MarkingMetadata => ({
  addedAt: attendance.departedAddedAt,
  addedBy: attendance.departedAddedBy,
  modifiedAt: attendance.departedModifiedAt,
  modifiedBy: attendance.departedModifiedBy
})

export function hasMarkingMetadata({
  addedAt,
  addedBy,
  modifiedAt,
  modifiedBy
}: MarkingMetadata): boolean {
  return (
    addedAt !== null ||
    addedBy !== null ||
    modifiedAt !== null ||
    modifiedBy !== null
  )
}

export function isModifiedAfterAdding({
  addedAt,
  addedBy,
  modifiedAt,
  modifiedBy
}: MarkingMetadata): boolean {
  return (
    modifiedAt !== null &&
    modifiedBy !== null &&
    (addedAt === null ||
      !addedAt.isEqual(modifiedAt) ||
      !isEqual(addedBy, modifiedBy))
  )
}

export function isModifiedByPersonAfterAdding(
  metadata: MarkingMetadata
): boolean {
  return (
    isModifiedAfterAdding(metadata) && metadata.modifiedBy?.type !== 'SYSTEM'
  )
}

export function isAddedOnLaterDate(
  { addedAt, addedBy }: MarkingMetadata,
  date: LocalDate
): boolean {
  return (
    addedAt !== null &&
    addedBy !== null &&
    addedBy.type !== 'SYSTEM' &&
    addedAt.toLocalDate().isAfter(date)
  )
}

export function isRetroactive(
  metadata: MarkingMetadata,
  date: LocalDate
): boolean {
  return (
    isModifiedByPersonAfterAdding(metadata) ||
    isAddedOnLaterDate(metadata, date)
  )
}

/**
 * Tooltip lines for one attendance time. The "added" line is marked with an
 * asterisk when it was added after `date`; a null `date` never marks it.
 */
export const MarkingTooltipContent = React.memo(function MarkingTooltipContent({
  metadata,
  date
}: {
  metadata: MarkingMetadata
  date: LocalDate | null
}) {
  const { i18n } = useTranslation()
  const { addedAt, addedBy, modifiedAt, modifiedBy } = metadata
  return (
    <>
      {addedAt !== null && addedBy !== null && (
        <P>
          {date !== null && isAddedOnLaterDate(metadata, date) && '*'}
          {i18n.unit.staffAttendance.addedAt} {addedAt.format()},{' '}
          <Italic>{addedBy.name}</Italic>
        </P>
      )}
      {modifiedAt !== null &&
        modifiedBy !== null &&
        isModifiedAfterAdding(metadata) && (
          <P>
            {isModifiedByPersonAfterAdding(metadata) && '*'}
            {i18n.unit.staffAttendance.modifiedAt} {modifiedAt.format()},{' '}
            <Italic>{modifiedBy.name}</Italic>
          </P>
        )}
    </>
  )
})
