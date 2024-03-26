// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { unitGroupDetailsQuery } from 'employee-frontend/components/unit/queries'
import { useTranslation } from 'employee-frontend/state/i18n'
import { combine } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { groupDiscussionReservationDaysQuery } from '../../queries'

import DiscussionTimesForm from './DiscussionTimesForm'

export const WidthLimiter = styled.div`
  max-width: 400px;
`
export type DiscussionSurveyEditMode = 'create' | 'edit'

const getCalendarHorizon = () => {
  const today = LocalDate.todayInSystemTz()
  const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

  const defaultHorizonDate = previousMonday.addMonths(1).lastDayOfMonth()
  return defaultHorizonDate
}

export default React.memo(function DiscussionSurveyEditor({
  unitId,
  groupId,
  eventData
}: {
  unitId: UUID
  groupId: UUID
  eventData: CalendarEvent | null
}) {
  const { i18n } = useTranslation()

  const horizonRef = useRef<HTMLDivElement | null>(null)
  const today = LocalDate.todayInSystemTz()

  const [calendarHorizonDate, setCalendarHorizonDate] =
    useState<LocalDate>(getCalendarHorizon())

  const maxCalendarRange = useMemo(
    () =>
      new FiniteDateRange(
        today.subDays(today.getIsoDayOfWeek() - 1),
        today.addMonths(5).lastDayOfMonth()
      ),
    [today]
  )

  const visibleCalendarRange = useMemo(() => {
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    return new FiniteDateRange(previousMonday, calendarHorizonDate)
  }, [calendarHorizonDate, today])

  const extendHorizon = useCallback(() => {
    const candidateHorizon = calendarHorizonDate.addMonths(1).lastDayOfMonth()
    if (candidateHorizon.isEqualOrBefore(maxCalendarRange.end)) {
      setCalendarHorizonDate(candidateHorizon)
      scrollRefIntoView(horizonRef, 80)
    }
  }, [maxCalendarRange, calendarHorizonDate])

  const groupData = useQueryResult(
    unitGroupDetailsQuery({
      unitId,
      from: maxCalendarRange.start,
      to: maxCalendarRange.end
    })
  )

  const calendarDays = useQueryResult(
    groupDiscussionReservationDaysQuery({
      unitId,
      groupId,
      start: maxCalendarRange.start,
      end: maxCalendarRange.end
    })
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {renderResult(
          combine(groupData, calendarDays),
          ([groupResult, calendarDaysResult]) => (
            <DiscussionTimesForm
              eventData={eventData}
              unitId={unitId}
              groupId={groupId}
              groupData={groupResult}
              horizonRef={horizonRef}
              calendarDays={calendarDaysResult}
              calendarRange={visibleCalendarRange}
              maxCalendarRange={maxCalendarRange}
              extendHorizonAction={extendHorizon}
            />
          )
        )}
      </ContentArea>
    </Container>
  )
})
