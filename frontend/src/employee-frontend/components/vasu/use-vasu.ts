// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Dispatch,
  SetStateAction,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'

import {
  useAutosave,
  AutosaveStatus
} from 'employee-frontend/utils/use-autosave'
import { Result } from 'lib-common/api'
import {
  Followup,
  FollowupEntry,
  GetVasuDocumentResponse,
  PermittedFollowupActions
} from 'lib-common/api-types/vasu'
import {
  ChildLanguage,
  VasuContent,
  VasuDocument
} from 'lib-common/generated/api-types/vasu'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { VasuTranslations, vasuTranslations } from 'lib-customizations/employee'

import {
  editFollowupEntry,
  EditFollowupEntryParams,
  getVasuDocument,
  putVasuDocument,
  PutVasuDocumentParams
} from './api'

type VasuMetadata = Omit<
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
  status: AutosaveStatus
  translations: VasuTranslations
  editFollowupEntry: (params: EditFollowupEntryParams) => void
  permittedFollowupActions: PermittedFollowupActions
}

export function useVasu(id: string): Vasu {
  const [vasu, setVasu] = useState<VasuMetadata>()
  const [content, setContent] = useState<VasuContent>({ sections: [] })
  const [childLanguage, setChildLanguage] = useState<ChildLanguage | null>(null)
  const [permittedFollowupActions, setPermittedFollowupActions] =
    useState<PermittedFollowupActions>({})

  const handleVasuDocLoaded = useCallback(
    ({
      vasu: { content, ...meta },
      permittedFollowupActions
    }: GetVasuDocumentResponse) => {
      setVasu(meta)
      setContent(content)
      setChildLanguage(meta.basics.childLanguage)
      setPermittedFollowupActions(permittedFollowupActions)
    },
    []
  )

  const getSaveParameters = useCallback(
    () =>
      [{ documentId: id, content, childLanguage }] as [PutVasuDocumentParams],
    [id, content, childLanguage]
  )

  const loadVasuDoc = useCallback(() => getVasuDocument(id), [id])

  const { status, setStatus, setDirty } = useAutosave({
    load: loadVasuDoc,
    onLoaded: handleVasuDocLoaded,
    save: putVasuDocument,
    getSaveParameters
  })

  const handleEditFollowupEntryResult = useCallback(
    (res: Result<unknown>) =>
      res.mapAll({
        loading: () => null,
        failure: () => setStatus((prev) => ({ ...prev, state: 'save-error' })),
        success: () =>
          setStatus((prev) => {
            if (prev.state === 'saving-dirty') {
              return { ...prev, state: 'dirty' }
            } else {
              void getVasuDocument(id).then((vasuRes) => {
                vasuRes.mapAll({
                  loading: () => null,
                  failure: () =>
                    setStatus((prev) => ({ ...prev, state: 'save-error' })),
                  success: handleVasuDocLoaded
                })
              })
              return { state: 'loading' }
            }
          })
      }),
    [setStatus, id, handleVasuDocLoaded]
  )
  const editFollowup = useRestApi(
    editFollowupEntry,
    handleEditFollowupEntryResult
  )

  useEffect(
    function addNewToPermittedFollowupActions() {
      content.sections.forEach((section) => {
        section.questions.forEach((question) => {
          if (question.type === 'FOLLOWUP') {
            const followup = question as Followup
            followup.value.forEach((entry: FollowupEntry) => {
              if (entry.id && !permittedFollowupActions[entry.id]) {
                setPermittedFollowupActions({
                  ...permittedFollowupActions,
                  [entry.id]: ['UPDATE']
                })
              }
            })
          }
        })
      })
    },
    [content, permittedFollowupActions]
  )

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
    status,
    translations,
    editFollowupEntry: editFollowup,
    permittedFollowupActions
  }
}
