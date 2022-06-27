// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SetStateAction, useCallback, useEffect, useState } from 'react'

import {
  AutosaveStatus,
  useAutosave
} from 'employee-frontend/utils/use-autosave'
import { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { UUID } from 'lib-common/types'

import { getAssistanceNeedDecision, putAssistanceNeedDecision } from './api'

export type AssistanceNeedDecisionInfo = {
  formState: AssistanceNeedDecision | undefined
  setFormState: React.Dispatch<
    SetStateAction<AssistanceNeedDecision | undefined>
  >
  status: AutosaveStatus
}

export function useAssistanceNeedDecision(
  childId: UUID,
  id: UUID
): AssistanceNeedDecisionInfo {
  const [formState, setFormState] = useState<AssistanceNeedDecision>()

  const getSaveParameters = useCallback(
    () =>
      [childId, id, formState] as [
        childId: string,
        id: string,
        data: AssistanceNeedDecision
      ],
    [childId, id, formState]
  )

  const loadDecision = useCallback(
    () => getAssistanceNeedDecision(childId, id),
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
