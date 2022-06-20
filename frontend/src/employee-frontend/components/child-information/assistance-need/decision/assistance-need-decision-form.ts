// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SetStateAction, useEffect, useState } from 'react'

import { Result } from 'lib-common/api'
import { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getAssistanceNeedDecision } from './api'

export type AssistanceNeedDecisionInfo = {
  assistanceNeedDecision: Result<AssistanceNeedDecision>
  reloadAssistanceNeedDecision: () => void
  formState: AssistanceNeedDecision | undefined
  setFormState: React.Dispatch<
    SetStateAction<AssistanceNeedDecision | undefined>
  >
}

export function useAssistanceNeedDecision(
  childId: UUID,
  id: UUID
): AssistanceNeedDecisionInfo {
  const [assistanceNeedDecision, reloadAssistanceNeedDecision] = useApiState(
    () => getAssistanceNeedDecision(childId, id),
    [childId, id]
  )

  const [formState, setFormState] = useState<AssistanceNeedDecision>()

  useEffect(() => {
    assistanceNeedDecision.mapAll({
      loading: () => null,
      failure: () => null, // TODO
      success: (result) => setFormState(result)
    })
  }, [assistanceNeedDecision])

  return {
    assistanceNeedDecision,
    reloadAssistanceNeedDecision,
    formState,
    setFormState
  }
}
