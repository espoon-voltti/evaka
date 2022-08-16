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

export interface CitizenVasuStatus {
  state: State
  savedAt?: Date
}

export type CitizenVasuMetadata = Omit<
  VasuDocument,
  | 'content'
  | 'authorsContent'
  | 'vasuDiscussionContent'
  | 'evaluationDiscussionContent'
>

interface CitizenVasu {
  vasu: CitizenVasuMetadata | undefined
  content: VasuContent
  childLanguage: ChildLanguage | null
  status: CitizenVasuStatus
  translations: VasuTranslations
  guardianHasGivenPermissionToShare: boolean
  setGuardianHasGivenPermissionToShare: Dispatch<SetStateAction<boolean>>
}

export function useVasu(id: string): CitizenVasu {
  const [status, setStatus] = useState<CitizenVasuStatus>({ state: 'loading' })
  const [vasu, setVasu] = useState<CitizenVasuMetadata>()
  const [content, setContent] = useState<VasuContent>({
    sections: [],
    hasDynamicFirstSection: false
  })
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
    childLanguage,
    status,
    translations,
    guardianHasGivenPermissionToShare,
    setGuardianHasGivenPermissionToShare
  }
}
