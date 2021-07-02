// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Dispatch,
  SetStateAction,
  useCallback,
  useEffect,
  useState
} from 'react'
import { Result } from '../../../lib-common/api'
import { isAutomatedTest } from '../../../lib-common/utils/helpers'
import { useDebouncedCallback } from '../../../lib-common/utils/useDebouncedCallback'
import { useRestApi } from '../../../lib-common/utils/useRestApi'
import {
  AuthorsContent,
  EvaluationDiscussionContent,
  getVasuDocument,
  putVasuDocument,
  PutVasuDocumentParams,
  VasuDiscussionContent,
  VasuDocument
} from './api'
import { VasuContent } from './vasu-content'

type State =
  | 'loading'
  | 'loading-error'
  | 'clean'
  | 'dirty'
  | 'saving'
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
  authorsContent: AuthorsContent
  setAuthorsContent: Dispatch<SetStateAction<AuthorsContent>>
  vasuDiscussionContent: VasuDiscussionContent
  setVasuDiscussionContent: Dispatch<SetStateAction<VasuDiscussionContent>>
  evaluationDiscussionContent: EvaluationDiscussionContent
  setEvaluationDiscussionContent: Dispatch<
    SetStateAction<EvaluationDiscussionContent>
  >
  status: VasuStatus
}

const debounceInterval = isAutomatedTest ? 200 : 2000

export function useVasu(id: string): Vasu {
  const [status, setStatus] = useState<VasuStatus>({ state: 'loading' })
  const [vasu, setVasu] = useState<VasuMetadata>()
  const [content, setContent] = useState<VasuContent>({ sections: [] })
  const [authorsContent, setAuthorsContent] = useState<AuthorsContent>({
    primaryAuthor: {
      name: '',
      title: '',
      phone: ''
    },
    otherAuthors: [
      {
        name: '',
        title: '',
        phone: ''
      }
    ]
  })
  const [
    vasuDiscussionContent,
    setVasuDiscussionContent
  ] = useState<VasuDiscussionContent>({
    discussionDate: null,
    participants: '',
    guardianViewsAndCollaboration: ''
  })
  const [
    evaluationDiscussionContent,
    setEvaluationDiscussionContent
  ] = useState<EvaluationDiscussionContent>({
    discussionDate: null,
    participants: '',
    guardianViewsAndCollaboration: '',
    evaluation: ''
  })

  useEffect(
    function loadVasuDocument() {
      setStatus({ state: 'loading' })
      void getVasuDocument(id).then((res) =>
        res.mapAll({
          loading: () => null,
          failure: () => setStatus({ state: 'loading-error' }),
          success: ({
            content,
            authorsContent,
            vasuDiscussionContent,
            evaluationDiscussionContent,
            ...meta
          }) => {
            setStatus({ state: 'clean' })
            setVasu(meta)
            setContent(content)
            setAuthorsContent(authorsContent)
            setVasuDiscussionContent(vasuDiscussionContent)
            setEvaluationDiscussionContent(evaluationDiscussionContent)
          }
        })
      )
    },
    [id]
  )

  const handleSaveResult = useCallback(
    (res: Result<unknown>) =>
      res.mapAll({
        loading: () => null,
        failure: () => setStatus((prev) => ({ ...prev, state: 'save-error' })),
        success: () => setStatus({ state: 'clean', savedAt: new Date() })
      }),
    []
  )
  const save = useRestApi(putVasuDocument, handleSaveResult)
  const saveNow = useCallback(
    (params: PutVasuDocumentParams) => {
      setStatus((prev) => ({ ...prev, state: 'saving' }))
      save(params)
    },
    [save]
  )
  const debouncedSave = useDebouncedCallback(saveNow, debounceInterval)

  useEffect(
    function saveDirtyContent() {
      if (status.state === 'dirty') {
        debouncedSave({
          documentId: id,
          content,
          authorsContent,
          vasuDiscussionContent,
          evaluationDiscussionContent
        })
      }
    },
    [
      debouncedSave,
      status.state,
      content,
      authorsContent,
      vasuDiscussionContent,
      evaluationDiscussionContent,
      id
    ]
  )

  const setContentCallback = useCallback(
    (draft: SetStateAction<VasuContent>) => {
      setContent(draft)
      setStatus((status) => ({ ...status, state: 'dirty' }))
    },
    []
  )

  const setAuthorsContentCallback = useCallback(
    (draft: SetStateAction<AuthorsContent>) => {
      setAuthorsContent(draft)
      setStatus((status) => ({ ...status, state: 'dirty' }))
    },
    []
  )

  const setVasuDiscussionContentCallback = useCallback(
    (draft: SetStateAction<VasuDiscussionContent>) => {
      setVasuDiscussionContent(draft)
      setStatus((status) => ({ ...status, state: 'dirty' }))
    },
    []
  )

  const setEvaluationDiscussionContentCallback = useCallback(
    (draft: SetStateAction<EvaluationDiscussionContent>) => {
      setEvaluationDiscussionContent(draft)
      setStatus((status) => ({ ...status, state: 'dirty' }))
    },
    []
  )

  return {
    vasu,
    content,
    setContent: setContentCallback,
    authorsContent,
    setAuthorsContent: setAuthorsContentCallback,
    vasuDiscussionContent,
    setVasuDiscussionContent: setVasuDiscussionContentCallback,
    evaluationDiscussionContent,
    setEvaluationDiscussionContent: setEvaluationDiscussionContentCallback,
    status
  }
}
