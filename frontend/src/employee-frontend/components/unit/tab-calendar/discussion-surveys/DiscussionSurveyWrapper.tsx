// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { CalendarEventId } from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import { constantQuery, useQueryResult } from 'lib-common/query'
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
  const {
    groupId,
    unitId,
    eventId: eventIdOrNew
  } = useRouteParams(['groupId', 'unitId', 'eventId'])
  const eventId =
    eventIdOrNew && eventIdOrNew !== 'new'
      ? fromUuid<CalendarEventId>(eventIdOrNew)
      : null

  const eventData = useQueryResult(
    eventId !== null
      ? discussionSurveyQuery({ id: eventId })
      : constantQuery(null)
  )

  return renderResult(eventData, (data) =>
    data != null ? (
      mode === 'EDIT' ? (
        <DiscussionSurveyEditor
          unitId={unitId}
          groupId={groupId}
          eventData={data}
        />
      ) : mode === 'VIEW' ? (
        <DiscussionSurveyView
          unitId={unitId}
          groupId={groupId}
          eventData={data}
        />
      ) : null
    ) : (
      <DiscussionSurveyEditor
        unitId={unitId}
        groupId={groupId}
        eventData={null}
      />
    )
  )
})
