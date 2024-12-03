// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext } from 'react'
import { useNavigate } from 'react-router'

import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

import FixedPeriodQuestionnaireForm from './FixedPeriodQuestionnaireForm'
import OpenRangesQuestionnaireForm from './OpenRangesQuestionnaireForm'
import { questionnaireQuery } from './queries'

export default React.memo(function QuestionnaireEditor() {
  const { id } = useRouteParams(['id'])
  const { user } = useContext(UserContext)

  const questionnaire = useQueryResult(questionnaireQuery({ id }), {
    enabled: id !== 'new'
  })

  const navigate = useNavigate()

  const navigateToList = useCallback(
    () => void navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {id === 'new' ? (
          user?.accessibleFeatures.openRangesHolidayQuestionnaire ? (
            <OpenRangesQuestionnaireForm
              onSuccess={navigateToList}
              onCancel={navigateToList}
            />
          ) : (
            <FixedPeriodQuestionnaireForm
              onSuccess={navigateToList}
              onCancel={navigateToList}
            />
          )
        ) : (
          renderResult(questionnaire, (questionnaire) =>
            questionnaire.type === 'FIXED_PERIOD' ? (
              <FixedPeriodQuestionnaireForm
                questionnaire={questionnaire}
                onSuccess={navigateToList}
                onCancel={navigateToList}
              />
            ) : questionnaire.type === 'OPEN_RANGES' ? (
              <OpenRangesQuestionnaireForm
                questionnaire={questionnaire}
                onSuccess={navigateToList}
                onCancel={navigateToList}
              />
            ) : (
              <div>Not Yet Implemented</div>
            )
          )
        )}
      </ContentArea>
    </Container>
  )
})
