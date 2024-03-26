// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'

import { discussionSurveyQuery } from '../queries'

import DiscussionSurveyView from './DiscussionSurveyView'
import DiscussionSurveyEditor from './survey-editor/DiscussionSurveyEditor'

type DiscussionReservationSurveyViewMode = 'VIEW' | 'EDIT'

export default React.memo(function DiscussionReservationSurveyWrapper({
  mode
}: {
  mode: DiscussionReservationSurveyViewMode
}) {
  const { groupId, unitId, eventId } = useRouteParams([
    'groupId',
    'unitId',
    'eventId'
  ])

  const hasExistingEvent = useMemo(
    () => !!eventId && eventId !== 'new',
    [eventId]
  )
  const eventData = useQueryResult(discussionSurveyQuery({ id: eventId }), {
    enabled: hasExistingEvent
  })

  if (hasExistingEvent) {
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
