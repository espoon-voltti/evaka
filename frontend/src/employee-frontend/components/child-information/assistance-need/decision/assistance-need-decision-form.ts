// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SetStateAction, useCallback, useEffect, useState } from 'react'

import {
  AutosaveStatus,
  useAutosave
} from 'employee-frontend/utils/use-autosave'
import { Result } from 'lib-common/api'
import {
  AssistanceNeedDecision,
  AssistanceNeedDecisionEmployee
} from 'lib-common/generated/api-types/assistanceneed'
import { UUID } from 'lib-common/types'

import { getAssistanceNeedDecision, putAssistanceNeedDecision } from './api'

type AssistanceNeedDecisionEmployeeForForm = Omit<
  AssistanceNeedDecisionEmployee,
  'name' | 'email'
>

export type AssistanceNeedDecisionForm = Omit<
  AssistanceNeedDecision,
  'selectedUnit' | 'preparedBy1' | 'preparedBy2' | 'decisionMaker'
> & {
  selectedUnit: { id: UUID | null } | null
  preparedBy1: AssistanceNeedDecisionEmployeeForForm | null
  preparedBy2: AssistanceNeedDecisionEmployeeForForm | null
  decisionMaker: Omit<
    AssistanceNeedDecisionEmployeeForForm,
    'phoneNumber'
  > | null
}

export type AssistanceNeedDecisionInfo = {
  formState: AssistanceNeedDecisionForm | undefined
  setFormState: React.Dispatch<
    SetStateAction<AssistanceNeedDecisionForm | undefined>
  >
  status: AutosaveStatus
}

export function useAssistanceNeedDecision(
  childId: UUID,
  id: UUID
): AssistanceNeedDecisionInfo {
  const [formState, setFormState] = useState<AssistanceNeedDecisionForm>()

  const getSaveParameters = useCallback(
    () =>
      [childId, id, formState] as [
        childId: string,
        id: string,
        data: AssistanceNeedDecisionForm
      ],
    [childId, id, formState]
  )

  const loadDecision = useCallback(
    () =>
      getAssistanceNeedDecision(childId, id).then(
        (d) => d as Result<AssistanceNeedDecisionForm>
      ),
    [childId, id]
  )

  const { status, setDirty } = useAutosave({
    load: loadDecision,
    onLoaded: setFormState,
    save: putAssistanceNeedDecision,
    getSaveParameters
  })

  // set dirty whenever formState changes
  useEffect(() => {
    setDirty()
  }, [setDirty, formState])

  return {
    formState,
    setFormState,
    status
  }
}
