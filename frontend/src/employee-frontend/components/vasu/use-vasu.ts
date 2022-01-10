// SPDX-FileCopyrightText: 2017-2021 City of Espoo
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
import { isAutomatedTest } from 'lib-common/utils/helpers'
import { useDebouncedCallback } from 'lib-common/utils/useDebouncedCallback'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { VasuTranslations, vasuTranslations } from 'lib-customizations/employee'
import {
  editFollowupEntry,
  EditFollowupEntryParams,
  getVasuDocument,
  putVasuDocument,
  PutVasuDocumentParams
} from './api'

type State =
  | 'loading'
  | 'loading-dirty'
  | 'loading-error'
  | 'clean'
  | 'dirty'
  | 'saving'
  | 'saving-dirty'
  | 'save-error'

export interface VasuStatus {
  state: State
  savedAt?: Date
}

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
  status: VasuStatus
  translations: VasuTranslations
  editFollowupEntry: (params: EditFollowupEntryParams) => void
  permittedFollowupActions: PermittedFollowupActions
}

const debounceInterval = isAutomatedTest ? 200 : 2000

export function useVasu(id: string): Vasu {
  const [status, setStatus] = useState<VasuStatus>({ state: 'loading' })
  const [vasu, setVasu] = useState<VasuMetadata>()
  const [content, setContent] = useState<VasuContent>({ sections: [] })
  const [childLanguage, setChildLanguage] = useState<ChildLanguage | null>(null)
  const [permittedFollowupActions, setPermittedFollowupActions] =
    useState<PermittedFollowupActions>({})

  const handleVasuDocLoaded = useCallback(
    (res: Result<GetVasuDocumentResponse>) => {
      res.mapAll({
        loading: () => null,
        failure: () => setStatus({ state: 'loading-error' }),
        success: ({ vasu: { content, ...meta }, permittedFollowupActions }) => {
          setVasu(meta)
          setContent(content)
          setChildLanguage(meta.basics.childLanguage)
          setPermittedFollowupActions(permittedFollowupActions)
          setStatus((prev) =>
            prev.state === 'loading-dirty'
              ? { ...prev, state: 'dirty' }
              : { state: 'clean' }
          )
        }
      })
    },
    []
  )

  useEffect(
    function loadVasuDocument() {
      setStatus({ state: 'loading' })
      void getVasuDocument(id).then(handleVasuDocLoaded)
    },
    [id, handleVasuDocLoaded]
  )

  const handleSaveResult = useCallback((res: Result<unknown>) => {
    res.mapAll({
      loading: () => null,
      failure: () => setStatus((prev) => ({ ...prev, state: 'save-error' })),
      success: () => {
        setStatus({ state: 'clean', savedAt: new Date() })
      }
    })
  }, [])
  const save = useRestApi(putVasuDocument, handleSaveResult)
  const saveNow = useCallback(
    (params: PutVasuDocumentParams) => {
      setStatus((prev) => ({ ...prev, state: 'saving' }))
      save(params)
    },
    [save]
  )
  const [debouncedSave] = useDebouncedCallback(saveNow, debounceInterval)

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
              void getVasuDocument(id).then(handleVasuDocLoaded)
              return { state: 'loading' }
            }
          })
      }),
    [id, handleVasuDocLoaded]
  )
  const editFollowup = useRestApi(
    editFollowupEntry,
    handleEditFollowupEntryResult
  )

  useEffect(
    function saveDirtyContent() {
      if (status.state === 'dirty') {
        debouncedSave({ documentId: id, content, childLanguage })
      }
    },
    [debouncedSave, status.state, content, childLanguage, id]
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

  const setDirty = useCallback(
    () =>
      setStatus((prev) => {
        const state =
          prev.state === 'loading'
            ? 'loading-dirty'
            : prev.state === 'saving'
            ? 'saving-dirty'
            : 'dirty'
        return { ...prev, state }
      }),
    []
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
