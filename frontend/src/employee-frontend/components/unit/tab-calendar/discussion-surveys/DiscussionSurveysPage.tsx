// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { isLoading } from 'lib-common/api'
import type { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1, H2 } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import { daycareQuery } from '../../queries'
import { groupDiscussionSurveysQuery } from '../queries'

import DiscussionReservationSurveyItem from './DiscussionSurveyItem'

const SurveyList = styled.ul`
  list-style-type: none;
  margin: 30px 0 0 0;
  padding: 0;
`

const ClickableSurvey = styled.div`
  margin: 0;
  padding: 0;
  cursor: pointer;
`

export default React.memo(function DiscussionReservationSurveysPage() {
  const { i18n } = useTranslation()
  const unitId = useIdRouteParam<DaycareId>('unitId')
  const groupId = useIdRouteParam<GroupId>('groupId')
  const unitInformation = useQueryResult(daycareQuery({ daycareId: unitId }))

  const discussionSurveys = useQueryResult(
    groupDiscussionSurveysQuery({ unitId, groupId })
  )
  const navigate = useNavigate()
  const t = i18n.unit.calendar.events

  const navigateToSurvey = useCallback(
    (surveyId: string) => {
      void navigate(
        `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys/${surveyId}`
      )
    },
    [navigate, unitId, groupId]
  )

  const sortedSurveys = useMemo(
    () =>
      discussionSurveys.map((surveys) =>
        orderBy(surveys, (s: CalendarEvent) => s.contentModifiedAt, ['desc'])
      ),
    [discussionSurveys]
  )

  return (
    <Container>
      <ReturnButton
        label={i18n.common.goBack}
        onClick={() => {
          void navigate(`/units/${unitId}/calendar/?group=${groupId}`)
        }}
      />
      <ContentArea opaque>
        {renderResult(unitInformation, ({ daycare, groups }) => (
          <>
            <H1>{daycare.name}</H1>
            <H2>{groups.find((g) => g.id === groupId)?.name}</H2>
          </>
        ))}
        <H2>{t.discussionReservation.discussionPageTitle}</H2>
        <p>{t.discussionReservation.discussionPageDescription}</p>
        <AddButtonRow
          onClick={() => navigateToSurvey('new')}
          text={t.discussionReservation.surveyCreate}
          data-qa="create-discussion-survey-button"
        />
        {renderResult(sortedSurveys, (events) => (
          <SurveyList
            data-qa="discussion-survey-list"
            data-isloading={isLoading(sortedSurveys)}
          >
            {events.map((e) => (
              <li key={e.id}>
                <ClickableSurvey onClick={() => navigateToSurvey(e.id)}>
                  <DiscussionReservationSurveyItem eventData={e} />
                </ClickableSurvey>
              </li>
            ))}
          </SurveyList>
        ))}
      </ContentArea>
    </Container>
  )
})
