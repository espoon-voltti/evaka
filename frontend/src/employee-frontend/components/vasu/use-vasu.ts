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
import { isAutomatedTest } from '../../../lib-common/utils/helpers'
import { useDebouncedCallback } from '../../../lib-common/utils/useDebouncedCallback'
import { useRestApi } from '../../../lib-common/utils/useRestApi'
import {
  getVasuDocument,
  putVasuDocument,
  PutVasuDocumentParams,
  VasuDocumentResponse
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

type VasuMetadata = Omit<VasuDocumentResponse, 'content'>

interface Vasu {
  vasu: VasuMetadata | undefined
  content: VasuContent
  setContent: Dispatch<SetStateAction<VasuContent>>
  status: VasuStatus
}

export function useVasu(id: string): Vasu {
  const [status, setStatus] = useState<VasuStatus>({ state: 'loading' })
  const [vasu, setVasu] = useState<VasuMetadata>()
  const [content, setContent] = useState<VasuContent>({ sections: [] })

  useEffect(() => {
    setStatus((prev) => ({ ...prev, state: 'loading' }))
    void getVasuDocument(id).then((res) => {
      if (res.isSuccess) {
        setStatus({ state: 'clean' })
        const { content, ...meta } = res.value
        setVasu(meta)
        setContent(content)
      } else if (res.isFailure) {
        setStatus({ state: 'loading-error' })
      }
    })
  }, [id])

  const save = useRestApi(putVasuDocument, (res) => {
    if (res.isSuccess) {
      setStatus({ state: 'clean', savedAt: new Date() })
    } else if (res.isFailure) {
      setStatus((prev) => ({ ...prev, state: 'save-error' }))
    }
  })
  const saveNow = useCallback(
    (params: PutVasuDocumentParams) => {
      setStatus((prev) => ({ ...prev, state: 'saving' }))
      save(params)
    },
    [save]
  )
  const debounceInterval = isAutomatedTest ? 200 : 2000
  const debouncedSave = useDebouncedCallback(saveNow, debounceInterval)

  // save dirty content with debounce
  useEffect(() => {
    if (status.state === 'dirty') {
      debouncedSave({ documentId: id, content })
    }
  }, [debouncedSave, status.state, content, id])

  const setContentCallback = useCallback(
    (draft: SetStateAction<VasuContent>) => {
      setContent(draft)
      setStatus((status) => ({ ...status, state: 'dirty' }))
    },
    []
  )

  return { vasu, content, setContent: setContentCallback, status }
}
