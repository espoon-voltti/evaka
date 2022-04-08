// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { Success } from 'lib-common/api'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import FixedPeriodQuestionnaireForm from './FixedPeriodQuestionnaireForm'
import { getQuestionnaire } from './api'

export default React.memo(function QuestionnaireEditor() {
  const { id } = useNonNullableParams<{ id: string }>()
  const questionnaireId = id === 'new' ? undefined : id

  const [questionnaire] = useApiState(
    () =>
      questionnaireId
        ? getQuestionnaire(questionnaireId)
        : Promise.resolve(Success.of(undefined)),
    [questionnaireId]
  )

  const navigate = useNavigate()

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(questionnaire, (questionnaire) => (
          <FixedPeriodQuestionnaireForm
            questionnaire={questionnaire}
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        ))}
      </ContentArea>
    </Container>
  )
})
