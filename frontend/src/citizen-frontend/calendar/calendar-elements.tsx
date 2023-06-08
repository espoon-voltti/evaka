// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import {
  ReservationResponseDay,
  ReservationResponseDayChild
} from 'lib-common/generated/api-types/reservations'
import {
  reservationHasTimes,
  reservationsAndAttendancesDiffer
} from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Light } from 'lib-components/typography'

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
    ({ reservations, attendances }) =>
      reservationsAndAttendancesDiffer(reservations, attendances)
  )

  const groupedChildren = useMemo(
    () => groupChildren(data.children),
    [data.children]
  )

  return data.children.length === 0 && data.holiday && !isReservable ? (
    <Holiday />
  ) : (
    <div>
      <FixedSpaceColumn spacing="xs">
        {groupedChildren.map((group) => (
          <FixedSpaceRow
            key={group.key}
            spacing="s"
            alignItems="center"
            data-qa="reservation-group"
          >
            <RoundChildImages
              images={childImages.filter((image) =>
                group.childIds.includes(image.childId)
              )}
            />
            <GroupedElementText
              $type={group.type}
              $backgroundHighlight={backgroundHighlight}
              data-qa="reservation-text"
            >
              {groupText(group, i18n)}
            </GroupedElementText>
          </FixedSpaceRow>
        ))}
      </FixedSpaceColumn>
      {showAttendanceWarning && <StatusIcon status="warning" />}
    </div>
  )
})

export const Holiday = React.memo(function Holiday() {
  const i18n = useTranslation()
  return <Light>{i18n.calendar.holiday}</Light>
})

const GroupedElementText = styled.div<{
  $type: DailyChildGroupElementType
  $backgroundHighlight: 'nonEditableAbsence' | 'holidayPeriod' | undefined
}>`
  word-break: break-word;

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

const groupChildren = (relevantChildren: ReservationResponseDayChild[]) =>
  Object.entries(
    groupBy(
      relevantChildren.map((child): DailyChildGroupElement => {
        if (child.attendances.length > 0) {
          return {
            childId: child.childId,
            type: 'attendance',
            text: child.attendances
              .map(
                ({ startTime, endTime }) =>
                  `${startTime.format()}–${endTime?.format() ?? ''}`
              )
              .join(', ')
          }
        }

        if (child.absence) {
          return {
            childId: child.childId,
            type:
              child.absence.type === 'FREE_ABSENCE' ? 'absent-free' : 'absent'
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
              .map(
                ({ startTime, endTime }) =>
                  `${startTime.format()}–${endTime.format()}`
              )
              .join(', ')
          }
        }

        if (!child.requiresReservation) {
          return {
            childId: child.childId,
            type: 'present'
          }
        }

        return {
          childId: child.childId,
          type: 'missing-reservation'
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
  }
}
