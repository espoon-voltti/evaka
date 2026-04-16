// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'fake-indexeddb/auto'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import * as draftStore from './draftStore'
import { useReplyDraftRestore } from './useReplyDraftRestore'

async function resetDb() {
  await new Promise<void>((resolve) => {
    const req = indexedDB.deleteDatabase('evaka-citizen-webpush')
    req.onsuccess = () => resolve()
    req.onerror = () => resolve()
    req.onblocked = () => resolve()
  })
}

describe('useReplyDraftRestore', () => {
  afterEach(async () => {
    vi.restoreAllMocks()
    await resetDb()
  })

  it('loads a saved draft and calls onRestore', async () => {
    await draftStore.saveDraft('thread-a', 'saved from notification')
    const onRestore = vi.fn()

    const { result } = renderHook(() =>
      useReplyDraftRestore('thread-a', onRestore)
    )

    await waitFor(() => {
      expect(onRestore).toHaveBeenCalledWith('saved from notification')
    })
    expect(result.current.restored).toBe(true)
  })

  it('does not flip restored when no draft exists', async () => {
    const loadSpy = vi.spyOn(draftStore, 'loadDraft')
    const onRestore = vi.fn()

    const { result } = renderHook(() =>
      useReplyDraftRestore('no-draft', onRestore)
    )

    await waitFor(() => expect(loadSpy).toHaveBeenCalledWith('no-draft'))
    // loadDraft has resolved — the effect has observed the null draft.
    expect(onRestore).not.toHaveBeenCalled()
    expect(result.current.restored).toBe(false)
  })

  it('discardDraft removes the IDB entry and resets restored', async () => {
    await draftStore.saveDraft('thread-b', 'discard me')
    const onRestore = vi.fn()
    const { result } = renderHook(() =>
      useReplyDraftRestore('thread-b', onRestore)
    )
    await waitFor(() => expect(result.current.restored).toBe(true))

    result.current.discardDraft()
    await waitFor(() => expect(result.current.restored).toBe(false))

    // Remount — IDB entry should be gone, so loadDraft resolves with null.
    const loadSpy = vi.spyOn(draftStore, 'loadDraft')
    const onRestore2 = vi.fn()
    renderHook(() => useReplyDraftRestore('thread-b', onRestore2))
    await waitFor(() => expect(loadSpy).toHaveBeenCalledWith('thread-b'))
    expect(onRestore2).not.toHaveBeenCalled()
  })
})
