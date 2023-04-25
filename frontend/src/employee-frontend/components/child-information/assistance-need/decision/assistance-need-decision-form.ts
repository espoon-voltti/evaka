// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { SetStateAction } from 'react'
import type React from 'react'
import { useCallback, useEffect, useState } from 'react'

import type { Employee } from 'employee-frontend/types/employee'
import type { AutosaveStatus } from 'employee-frontend/utils/use-autosave'
import { useAutosave } from 'employee-frontend/utils/use-autosave'
import type { Result } from 'lib-common/api'
import type { AssistanceNeedDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import type { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import {
  getAssistanceDecisionMakerOptions,
  getAssistanceNeedDecision,
  putAssistanceNeedDecision
} from './api'

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
      getAssistanceNeedDecision(id).then(
        (d) =>
          d.map(
            ({ decision }) => decision
          ) as Result<AssistanceNeedDecisionForm>
      ),
    [id]
  )

  const [decisionMakerOptions, reloadDecisionMakerOptions] = useApiState(
    () => getAssistanceDecisionMakerOptions(id),
    [id]
  )

  const { status, setDirty, forceSave } = useAutosave({
    load: loadDecision,
    onLoaded: setFormState,
    save: (id: UUID, decision: AssistanceNeedDecisionForm) =>
      putAssistanceNeedDecision(id, decision).then((result) =>
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
