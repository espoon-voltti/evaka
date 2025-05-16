// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { SetStateAction } from 'react'
import type React from 'react'
import { useCallback, useEffect, useState } from 'react'

import type { Result } from 'lib-common/api'
import { wrapResult } from 'lib-common/api'
import type { AssistanceNeedDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import type { Employee } from 'lib-common/generated/api-types/pis'
import type { AssistanceNeedDecisionId } from 'lib-common/generated/api-types/shared'
import { useApiState } from 'lib-common/utils/useRestApi'

import {
  getAssistanceDecisionMakerOptions,
  getAssistanceNeedDecision,
  updateAssistanceNeedDecision
} from '../../../../generated/api-clients/assistanceneed'
import type { AutosaveStatus } from '../../../../utils/use-autosave'
import { useAutosave } from '../../../../utils/use-autosave'

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
  id: AssistanceNeedDecisionId
): AssistanceNeedDecisionInfo {
  const [formState, setFormState] = useState<AssistanceNeedDecisionForm>()

  const getSaveParameters = useCallback(
    (): [AssistanceNeedDecisionId, AssistanceNeedDecisionForm] => [
      id,
      formState!
    ],
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
    save: (
      id: AssistanceNeedDecisionId,
      decision: AssistanceNeedDecisionForm
    ) =>
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
