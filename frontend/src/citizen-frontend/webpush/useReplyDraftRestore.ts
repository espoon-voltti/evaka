// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useState } from 'react'

import { deleteDraft, loadDraft, purgeOldDrafts } from './draftStore'

const DRAFT_TTL_MS = 1000 * 60 * 60 * 24 * 7 // 7 days

/**
 * On mount, loads any saved push-notification reply draft for `threadId` and
 * calls `onRestore` with the text. Also runs a best-effort purge of drafts
 * older than DRAFT_TTL_MS. Returns a boolean that flips true once a draft has
 * been restored so the UI can render a "restored from notification" hint.
 */
export function useReplyDraftRestore(
  threadId: string,
  onRestore: (text: string) => void
): { restored: boolean; clearRestored: () => void; discardDraft: () => void } {
  const [restored, setRestored] = useState(false)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        await purgeOldDrafts(DRAFT_TTL_MS)
        const draft = await loadDraft(threadId)
        if (cancelled || !draft) return
        onRestore(draft.text)
        setRestored(true)
      } catch {
        // IDB unavailable — no draft to restore, not fatal.
      }
    })()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [threadId])

  return {
    restored,
    clearRestored: () => setRestored(false),
    discardDraft: () => {
      void deleteDraft(threadId).catch(() => {})
      setRestored(false)
    }
  }
}
