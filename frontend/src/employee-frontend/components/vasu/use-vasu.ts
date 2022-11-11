// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Dispatch, SetStateAction, useCallback, useMemo, useState } from 'react'

import {
  useAutosave,
  AutosaveStatus
} from 'employee-frontend/utils/use-autosave'
import { Action } from 'lib-common/generated/action'
import {
  ChildLanguage,
  VasuContent,
  VasuDocument,
  VasuDocumentWithPermittedActions
} from 'lib-common/generated/api-types/vasu'
import { VasuTranslations, vasuTranslations } from 'lib-customizations/employee'

import { getVasuDocument, putVasuDocument, PutVasuDocumentParams } from './api'

export type VasuMetadata = Omit<
  VasuDocument,
  | 'content'
  | 'authorsContent'
  | 'vasuDiscussionContent'
  | 'evaluationDiscussionContent'
>

interface Vasu {
  vasu: VasuMetadata | undefined
  content: VasuContent
  setContent: Dispatch<SetStateAction<VasuContent>>
  childLanguage: ChildLanguage | null
  setChildLanguage: Dispatch<ChildLanguage>
  permittedActions: Action.VasuDocument[]
  status: AutosaveStatus
  translations: VasuTranslations
}

export function useVasu(id: string): Vasu {
  const [vasu, setVasu] = useState<VasuMetadata>()
  const [content, setContent] = useState<VasuContent>({
    sections: [],
    hasDynamicFirstSection: false
  })
  const [childLanguage, setChildLanguage] = useState<ChildLanguage | null>(null)
  const [permittedActions, setPermittedActions] = useState<
    Action.VasuDocument[]
  >([])

  const handleVasuDocLoaded = useCallback(
    ({
      data: { content, ...meta },
      permittedActions
    }: VasuDocumentWithPermittedActions) => {
      setVasu(meta)
      setContent(content)
      setChildLanguage(meta.basics.childLanguage)
      setPermittedActions(permittedActions)
    },
    []
  )

  const getSaveParameters: () => [PutVasuDocumentParams] = useCallback(
    () => [{ documentId: id, content, childLanguage }],
    [id, content, childLanguage]
  )

  const loadVasuDoc = useCallback(() => getVasuDocument(id), [id])

  const { status, setDirty } = useAutosave({
    load: loadVasuDoc,
    onLoaded: handleVasuDocLoaded,
    save: putVasuDocument,
    getSaveParameters
  })

  const setContentCallback = useCallback(
    (draft: SetStateAction<VasuContent>) => {
      setContent(draft)
      setDirty()
    },
    [setDirty]
  )

  const setChildLanguageCallback = useCallback(
    (childLanguage: ChildLanguage) => {
      setChildLanguage(childLanguage)
      setDirty()
    },
    [setDirty]
  )

  const translations = useMemo(
    () =>
      vasu !== undefined
        ? vasuTranslations[vasu.language]
        : vasuTranslations.FI,
    [vasu]
  )

  return {
    vasu,
    content,
    setContent: setContentCallback,
    childLanguage,
    setChildLanguage: setChildLanguageCallback,
    permittedActions,
    status,
    translations
  }
}
