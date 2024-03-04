// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SetStateAction, useCallback, useEffect, useState } from 'react'

import {
  AutosaveStatus,
  useAutosave
} from 'employee-frontend/utils/use-autosave'
import { Result, wrapResult } from 'lib-common/api'
import { AssistanceNeedDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import { Employee } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import {
  getAssistanceDecisionMakerOptions,
  getAssistanceNeedDecision,
  updateAssistanceNeedDecision
} from '../../../../generated/api-clients/assistanceneed'

const getAssistanceDecisionMakerOptionsResult = wrapResult(
  getAssistanceDecisionMakerOptions
)
const getAssistanceNeedDecisionResult = wrapResult(getAssistanceNeedDecision)
const updateAssistanceNeedDecisionResult = wrapResult(
  updateAssistanceNeedDecision
)

export type AssistanceNeedDecisionInfo = {
  decisionMakerOptions: Result<Employee[]>
  formState: AssistanceNeedDecisionForm | undefined
  setFormState: React.Dispatch<
    SetStateAction<AssistanceNeedDecisionForm | undefined>
  >
  status: AutosaveStatus
  forceSave: () => Promise<Result<void>>
}

export function useAssistanceNeedDecision(
  id: UUID
): AssistanceNeedDecisionInfo {
  const [formState, setFormState] = useState<AssistanceNeedDecisionForm>()

  const getSaveParameters = useCallback(
    () => [id, formState] as [id: string, data: AssistanceNeedDecisionForm],
    [id, formState]
  )

  const loadDecision = useCallback(
    () =>
      getAssistanceNeedDecisionResult({ id }).then(
        (d) =>
          d.map(
            ({ decision }) => decision
          ) as Result<AssistanceNeedDecisionForm>
      ),
    [id]
  )

  const [decisionMakerOptions, reloadDecisionMakerOptions] = useApiState(
    () => getAssistanceDecisionMakerOptionsResult({ id }),
    [id]
  )

  const { status, setDirty, forceSave } = useAutosave({
    load: loadDecision,
    onLoaded: setFormState,
    save: (id: UUID, decision: AssistanceNeedDecisionForm) =>
      updateAssistanceNeedDecisionResult({ id, body: { decision } }).then(
        (result) =>
          reloadDecisionMakerOptions()
            .then(() => result)
            .catch(() => result)
      ),
    getSaveParameters
  })

  // set dirty whenever formState changes
  useEffect(() => {
    setDirty()
  }, [setDirty, formState])

  return {
    decisionMakerOptions,
    formState,
    setFormState,
    status,
    forceSave
  }
}
