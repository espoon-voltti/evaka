// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled, { css } from 'styled-components'

import { mapScheduleType } from 'lib-common/api-types/placement'
import {
  ReservableTimeRange,
  Reservation,
  ReservationResponseDay,
  ReservationResponseDayChild
} from 'lib-common/generated/api-types/reservations'
import {
  reservationHasTimes,
  reservationsAndAttendancesDiffer
} from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Light } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/citizen'

import { Translations, useTranslation } from '../localization'

import RoundChildImages, { ChildImageData } from './RoundChildImages'

export const Reservations = React.memo(function Reservations({
  data,
  childImages,
  backgroundHighlight,
  isReservable
}: {
  data: ReservationResponseDay
  childImages: ChildImageData[]
  backgroundHighlight: 'nonEditableAbsence' | 'holidayPeriod' | undefined
  isReservable: boolean
}) {
  const i18n = useTranslation()
  const showAttendanceWarning = data.children.some(
    ({ reservations, attendances, usedService }) =>
      (usedService != null &&
        usedService.usedServiceMinutes > usedService.reservedMinutes) ||
      reservationsAndAttendancesDiffer(reservations, attendances)
  )

  const groupedChildren = useMemo(
    () => groupChildren(data.children, i18n),
    [data.children, i18n]
  )

  return data.children.length === 0 && data.holiday && !isReservable ? (
    <Holiday />
  ) : (
    <div>
      <FixedSpaceColumn spacing="xs">
        {groupedChildren.map((group) => {
          const text = groupText(group, i18n)
          const wordCount = text?.split(' ').length ?? 0
          return (
            <FixedSpaceRow
              key={group.key}
              spacing="xs"
              alignItems="center"
              data-qa="reservation-group"
            >
              <RoundChildImages
                images={childImages.filter((image) =>
                  group.childIds.includes(image.childId)
                )}
              />
              {group.childIds.length === 1 && wordCount > 2 ? (
                <Tooltip tooltip={text}>
                  <GroupedElementText
                    $type={group.type}
                    $backgroundHighlight={backgroundHighlight}
                    $clamp={true}
                    data-qa="reservation-text"
                  >
                    {text}
                  </GroupedElementText>
                </Tooltip>
              ) : (
                <GroupedElementText
                  $type={group.type}
                  $backgroundHighlight={backgroundHighlight}
                  data-qa="reservation-text"
                >
                  {text}
                </GroupedElementText>
              )}
            </FixedSpaceRow>
          )
        })}
      </FixedSpaceColumn>
      {showAttendanceWarning && <StatusIcon status="warning" />}
    </div>
  )
})

export const Holiday = React.memo(function Holiday() {
  const i18n = useTranslation()
  return <Light data-qa="holiday">{i18n.calendar.holiday}</Light>
})

const GroupedElementText = styled.div<{
  $type: DailyChildGroupElementType
  $backgroundHighlight: 'nonEditableAbsence' | 'holidayPeriod' | undefined
  $clamp?: boolean
}>`
  word-break: break-word;
  ${(p) =>
    p.$clamp &&
    css`
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    `}

  ${(p) =>
    p.$type === 'missing-reservation' &&
    p.$backgroundHighlight !== 'holidayPeriod'
      ? `color: ${p.theme.colors.accents.a2orangeDark};`
      : undefined}
`

type DailyChildGroupElementType =
  | 'attendance'
  | 'reservation'
  | 'present'
  | 'absent'
  | 'missing-reservation'
  | 'absent-free'
  | 'absent-planned'

interface DailyChildGroupElement {
  type: DailyChildGroupElementType
  text?: string
  childId: UUID
}

interface GroupedDailyChildren {
  type: DailyChildGroupElementType
  text?: string
  childIds: UUID[]
  key: string
}

const groupChildren = (
  relevantChildren: ReservationResponseDayChild[],
  i18n: Translations
) =>
  Object.entries(
    groupBy(
      relevantChildren.map((child): DailyChildGroupElement => {
        if (child.attendances.length > 0) {
          return {
            childId: child.childId,
            type: 'attendance',
            text: child.attendances.map((a) => a.format()).join(', ')
          }
        }

        if (child.absence) {
          return {
            childId: child.childId,
            type:
              child.absence.type === 'FREE_ABSENCE'
                ? 'absent-free'
                : featureFlags.citizenAttendanceSummary &&
                    child.absence.type === 'PLANNED_ABSENCE'
                  ? 'absent-planned'
                  : 'absent'
          }
        }

        if (child.reservations.length > 0) {
          const [withTimes, withoutTimes] = partition(
            child.reservations,
            reservationHasTimes
          )

          if (withoutTimes.length > 0) {
            // In theory, we could have reservations with and without times, but in practice this shouldn't happen
            return {
              childId: child.childId,
              type: 'present'
            }
          }

          return {
            childId: child.childId,
            type: 'reservation',
            text: withTimes
              .map((reservation) =>
                formatReservation(reservation, child.reservableTimeRange, i18n)
              )
              .join(', ')
          }
        }

        return {
          childId: child.childId,
          type: mapScheduleType(child.scheduleType, {
            RESERVATION_REQUIRED: () => 'missing-reservation',
            FIXED_SCHEDULE: () => 'present',
            TERM_BREAK: () => 'absent'
          })
        }
      }),
      ({ type, text }) => `${type},${text ?? ''}`
    )
  ).map(
    ([key, children]): GroupedDailyChildren => ({
      type: children[0].type,
      text: children[0].text,
      childIds: children.map(({ childId }) => childId),
      key
    })
  )

function groupText(group: GroupedDailyChildren, i18n: Translations) {
  switch (group.type) {
    case 'attendance':
    case 'reservation':
      return group.text
    case 'present':
      return i18n.calendar.reservationNoTimes
    case 'absent':
      return i18n.calendar.absent
    case 'missing-reservation':
      return i18n.calendar.missingReservation
    case 'absent-free':
      return i18n.calendar.absentFree
    case 'absent-planned':
      return i18n.calendar.absentPlanned
  }
}

export const formatReservation = (
  reservation: Reservation.Times,
  reservableTimeRange: ReservableTimeRange,
  i18n: Translations
) => {
  const timeOutput = reservation.range.format()

  if (!featureFlags.intermittentShiftCare) {
    return timeOutput
  } else {
    const showIntermittentShiftCareNotice =
      reservableTimeRange.type === 'INTERMITTENT_SHIFT_CARE' &&
      (reservableTimeRange.placementUnitOperationTime === null ||
        !reservableTimeRange.placementUnitOperationTime.contains(
          reservation.range
        ))

    return showIntermittentShiftCareNotice
      ? `${timeOutput} ${i18n.calendar.intermittentShiftCareNotification}`
      : timeOutput
  }
}
