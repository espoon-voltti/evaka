// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import {
  ChildDailyData,
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import { reservationsAndAttendancesDiffer } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Light } from 'lib-components/typography'

import { useTranslation } from '../localization'

import RoundChildImages, { ChildImageData } from './RoundChildImages'

type DailyChildGroupElementType =
  | 'attendance'
  | 'reservation'
  | 'day-off' // TODO: EVAKA-4287
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

export const Reservations = React.memo(function Reservations({
  data,
  allChildren,
  childImages
}: {
  data: DailyReservationData
  allChildren: ReservationChild[]
  childImages: ChildImageData[]
}) {
  const i18n = useTranslation()
  const showAttendanceWarning = data.children.some(
    ({ reservations, attendances }) =>
      reservationsAndAttendancesDiffer(reservations, attendances)
  )

  const groupedChildren = useMemo(
    () => groupChildren(allChildren, data.children),
    [data.children, allChildren]
  )

  return data.children.length === 0 && data.isHoliday ? (
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
              imageSize={32}
              imageBorder={2}
              imageOverlap={16}
            />
            <GroupedElementText
              className={group.type}
              data-qa="reservation-text"
            >
              {group.type === 'missing-reservation'
                ? i18n.calendar.noReservation
                : group.type === 'absent'
                ? i18n.calendar.absent
                : group.type === 'absent-free'
                ? i18n.calendar.absentFree
                : group.text}
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

export const NoReservation = React.memo(function NoReservation() {
  const i18n = useTranslation()
  return <NoReservationNote>{i18n.calendar.noReservation}</NoReservationNote>
})

const NoReservationNote = styled.span`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

const GroupedElementText = styled.div`
  &.missing-reservation {
    color: ${(p) => p.theme.colors.accents.a2orangeDark};
  }
`

const groupChildren = (
  allChildren: ReservationChild[],
  reservedChildren: ChildDailyData[]
) =>
  Object.entries(
    groupBy(
      allChildren.map<DailyChildGroupElement>((childInfo) => {
        const child = reservedChildren.find(
          ({ childId }) => childId === childInfo.id
        )

        if (!child) {
          return {
            childId: childInfo.id,
            type: 'missing-reservation'
          }
        }

        if (child.attendances.length > 0) {
          return {
            childId: child.childId,
            type: 'attendance',
            text: child.attendances
              .map(({ startTime, endTime }) => `${startTime}–${endTime ?? ''}`)
              .join(', ')
          }
        }

        if (child.absence) {
          return {
            childId: child.childId,
            type: child.absence === 'FREE_ABSENCE' ? 'absent-free' : 'absent'
          }
        }

        if (child.reservations.length > 0) {
          return {
            childId: child.childId,
            type: 'reservation',
            text: child.reservations
              .map(({ startTime, endTime }) => `${startTime}–${endTime}`)
              .join(', ')
          }
        }

        return {
          childId: child.childId,
          type: 'missing-reservation'
        }
      }),
      ({ type, text }) => JSON.stringify([type, text])
    )
  ).map<GroupedDailyChildren>(([key, children]) => ({
    type: children[0].type,
    text: children[0].text,
    childIds: children.map(({ childId }) => childId),
    key
  }))
