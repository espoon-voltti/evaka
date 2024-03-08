// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import FixedPeriodQuestionnaireForm from './FixedPeriodQuestionnaireForm'
import { questionnaireQuery } from './queries'

export default React.memo(function QuestionnaireEditor() {
  const { id } = useRouteParams(['id'])

  const questionnaire = useQueryResult(questionnaireQuery({ id }), {
    enabled: id !== 'new'
  })

  const navigate = useNavigate()

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {id === 'new' ? (
          <FixedPeriodQuestionnaireForm
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        ) : (
          renderResult(questionnaire, (questionnaire) => (
            <FixedPeriodQuestionnaireForm
              questionnaire={questionnaire}
              onSuccess={navigateToList}
              onCancel={navigateToList}
            />
          ))
        )}
      </ContentArea>
    </Container>
  )
})
