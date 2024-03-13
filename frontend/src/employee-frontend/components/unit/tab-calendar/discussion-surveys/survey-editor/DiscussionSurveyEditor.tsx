// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { unitGroupDetailsQuery } from 'employee-frontend/components/unit/queries'
import { useTranslation } from 'employee-frontend/state/i18n'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'

import DiscussionTimesForm from './DiscussionTimesForm'

export const WidthLimiter = styled.div`
  max-width: 400px;
`
export type DiscussionSurveyEditMode = 'create' | 'edit'

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

  const getCalendarHorizon = () => {
    const today = LocalDate.todayInSystemTz()
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    const defaultHorizonDate = previousMonday.addMonths(3).lastDayOfMonth()
    return defaultHorizonDate
  }

  const [calendarHorizonDate, setCalendarHorizonDate] =
    useState<LocalDate>(getCalendarHorizon())

  const calendarRange = useMemo(() => {
    const today = LocalDate.todayInSystemTz()
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    return new FiniteDateRange(previousMonday, calendarHorizonDate)
  }, [calendarHorizonDate])

  const groupData = useQueryResult(
    unitGroupDetailsQuery(unitId, calendarRange.start, calendarRange.end)
  )

  const extendHorizon = () => {
    setCalendarHorizonDate(calendarHorizonDate.addMonths(1).lastDayOfMonth())
    scrollRefIntoView(horizonRef, 200)
  }
  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {renderResult(groupData, (groupResult) => (
          <DiscussionTimesForm
            eventData={eventData}
            unitId={unitId}
            groupId={groupId}
            groupData={groupResult}
            horizonRef={horizonRef}
            calendarRange={calendarRange}
            extendHorizonAction={extendHorizon}
          />
        ))}
      </ContentArea>
    </Container>
  )
})
