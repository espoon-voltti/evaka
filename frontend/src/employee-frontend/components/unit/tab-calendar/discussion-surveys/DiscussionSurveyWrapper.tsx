// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'

import { discussionSurveyQuery } from '../queries'

import DiscussionSurveyView from './DiscussionSurveyView'
import DiscussionSurveyEditor from './survey-editor/DiscussionSurveyEditor'

type DiscussionReservationSurveyViewMode = 'VIEW' | 'EDIT'

export default React.memo(function DiscussionReservationSurveyWrapper({
  mode
}: {
  mode: DiscussionReservationSurveyViewMode
}) {
  const { groupId, unitId, eventId } = useNonNullableParams<{
    groupId: UUID
    unitId: UUID
    eventId: UUID
  }>()

  const existingEvent = useMemo(() => !!eventId && eventId !== 'new', [eventId])
  const eventData = useQueryResult(discussionSurveyQuery(eventId), {
    enabled: existingEvent
  })

  if (existingEvent) {
    if (mode === 'EDIT') {
      return renderResult(eventData, (data) => (
        <DiscussionSurveyEditor
          unitId={unitId}
          groupId={groupId}
          eventData={data}
        />
      ))
    } else if (mode === 'VIEW') {
      return renderResult(eventData, (data) => (
        <DiscussionSurveyView
          unitId={unitId}
          groupId={groupId}
          eventData={data}
        />
      ))
    } else {
      return null
    }
  } else {
    return (
      <DiscussionSurveyEditor
        unitId={unitId}
        groupId={groupId}
        eventData={null}
      />
    )
  }
})
