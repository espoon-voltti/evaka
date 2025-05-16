// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useState } from 'react'

import type { UpdatableDraftContent } from 'lib-common/generated/api-types/messaging'
import type {
  MessageAccountId,
  MessageDraftId
} from 'lib-common/generated/api-types/shared'
import { useMutation } from 'lib-common/query'
import { isAutomatedTest } from 'lib-common/utils/helpers'
import { useDebouncedCallback } from 'lib-common/utils/useDebouncedCallback'
import type { SaveDraftParams } from 'lib-components/messages/types'

import { initDraftMutation, saveDraftMutation } from './queries'

type SaveState = 'clean' | 'dirty' | 'saving'

export type Draft = UpdatableDraftContent & { accountId: MessageAccountId }

const draftToSaveParams = (
  { accountId, ...content }: Draft,
  id: MessageDraftId
) => ({
  content,
  accountId,
  draftId: id
})

export function useDraft(initialId: MessageDraftId | null): {
  draftId: MessageDraftId | null
  saveDraft: () => void
  wasModified: boolean
  state: 'clean' | 'dirty' | 'saving'
  setDraft: (draft: Draft) => void
} {
  const [saveState, setSaveState] = useState<SaveState>('clean')
  const [id, setId] = useState<MessageDraftId | null>(initialId)
  const [draft, setDraft] = useState<Draft>()
  const [wasModified, setWasModified] = useState(false)

  const { mutateAsync: initDraft } = useMutation(initDraftMutation)
  const { mutateAsync: saveDraft } = useMutation(saveDraftMutation)

  // initialize draft when needed
  const [initializing, setInitializing] = useState<boolean>(false)
  useEffect(() => {
    if (!id && saveState === 'dirty' && !initializing && draft) {
      setInitializing(true)
      void initDraft({ accountId: draft.accountId }).then((draftId) => {
        setId(draftId)
        setInitializing(false)
      })
    }
  }, [id, initDraft, draft, saveState, initializing])

  const saveNow = useCallback(
    (params: SaveDraftParams) => {
      setSaveState('saving')
      void saveDraft({
        draftId: params.draftId,
        accountId: params.accountId,
        body: params.content
      }).then(() => {
        setWasModified(true)
        setSaveState('clean')
      })
    },
    [saveDraft]
  )
  const debounceInterval = isAutomatedTest ? 200 : 2000
  const [debouncedSave] = useDebouncedCallback(saveNow, debounceInterval)

  // save dirty draft with debounce
  useEffect(() => {
    if (id && draft && saveState === 'dirty') {
      debouncedSave(draftToSaveParams(draft, id))
    }
  }, [draft, id, debouncedSave, saveState])

  const saveDraftCallback = useCallback(() => {
    if (id && draft) {
      saveNow(draftToSaveParams(draft, id))
    }
  }, [draft, id, saveNow])

  const setDraftCallback = useCallback((draft: Draft) => {
    setDraft(draft)
    setSaveState('dirty')
  }, [])

  return {
    draftId: id,
    state: saveState,
    setDraft: setDraftCallback,
    saveDraft: saveDraftCallback,
    wasModified
  }
}
