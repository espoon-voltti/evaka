// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useState } from 'react'
import { Result } from 'lib-common/api'
import { UpsertableDraftContent } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { isAutomatedTest } from 'lib-common/utils/helpers'
import { useDebouncedCallback } from 'lib-common/utils/useDebouncedCallback'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { SaveDraftParams } from 'lib-components/employee/messages/types'

type SaveState = 'clean' | 'dirty' | 'saving'

export type Draft = UpsertableDraftContent & { accountId: UUID }

const draftToSaveParams = ({ accountId, ...content }: Draft, id: string) => ({
  content,
  accountId,
  draftId: id
})

export function useDraft({
  initialId,
  saveDraftRaw,
  initDraftRaw
}: {
  initialId: UUID | null
  saveDraftRaw: (params: SaveDraftParams) => Promise<Result<void>>
  initDraftRaw: (accountId: string) => Promise<Result<string>>
}): {
  draftId: string | null
  saveDraft: () => void
  wasModified: boolean
  state: 'clean' | 'dirty' | 'saving'
  setDraft: (draft: Draft) => void
} {
  const [saveState, setSaveState] = useState<SaveState>('clean')
  const [id, setId] = useState<UUID | null>(initialId)
  const [draft, setDraft] = useState<Draft>()
  const [wasModified, setWasModified] = useState(false)

  const initDraft = useRestApi(initDraftRaw, (res: Result<UUID>) => {
    if (res.isSuccess) {
      setId(res.value)
      setInitializing(false)
    }
  })
  // initialize draft when needed
  const [initializing, setInitializing] = useState<boolean>(false)
  useEffect(() => {
    if (!id && saveState === 'dirty' && !initializing && draft) {
      setInitializing(true)
      initDraft(draft.accountId)
    }
  }, [id, initDraft, draft, saveState, initializing])

  const save = useRestApi(saveDraftRaw, (res: Result<void>) => {
    if (res.isSuccess) {
      setWasModified(true)
      setSaveState('clean')
    }
  })
  const saveNow = useCallback(
    (params: SaveDraftParams) => {
      setSaveState('saving')
      save(params)
    },
    [save]
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
