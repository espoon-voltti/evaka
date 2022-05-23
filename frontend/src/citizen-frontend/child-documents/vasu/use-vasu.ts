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

import { Result } from 'lib-common/api'
import {
  ChildLanguage,
  CitizenGetVasuDocumentResponse,
  VasuContent,
  VasuDocument
} from 'lib-common/generated/api-types/vasu'
import { VasuTranslations, vasuTranslations } from 'lib-customizations/employee'

import { getCitizenVasuDocument } from './api'

type State = 'loading' | 'loading-dirty' | 'loading-error' | 'clean' | 'dirty'

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
  status: VasuStatus
  translations: VasuTranslations
  guardianHasGivenPermissionToShare: boolean
  setGuardianHasGivenPermissionToShare: Dispatch<SetStateAction<boolean>>
}

export function useVasu(id: string): Vasu {
  const [status, setStatus] = useState<VasuStatus>({ state: 'loading' })
  const [vasu, setVasu] = useState<VasuMetadata>()
  const [content, setContent] = useState<VasuContent>({ sections: [] })
  const [childLanguage, setChildLanguage] = useState<ChildLanguage | null>(null)
  const [
    guardianHasGivenPermissionToShare,
    setGuardianHasGivenPermissionToShare
  ] = useState<boolean>(false)

  const handleVasuDocLoaded = useCallback(
    (res: Result<CitizenGetVasuDocumentResponse>) => {
      res.mapAll({
        loading: () => null,
        failure: () => setStatus({ state: 'loading-error' }),
        success: ({
          vasu: { content, ...meta },
          guardianHasGivenPermissionToShare
        }) => {
          setVasu(meta)
          setContent(content)
          setGuardianHasGivenPermissionToShare(
            guardianHasGivenPermissionToShare
          )
          setChildLanguage(meta.basics.childLanguage)
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
      void getCitizenVasuDocument(id).then(handleVasuDocLoaded)
    },
    [id, handleVasuDocLoaded]
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
    setContent,
    childLanguage,
    status,
    translations,
    guardianHasGivenPermissionToShare,
    setGuardianHasGivenPermissionToShare
  }
}
