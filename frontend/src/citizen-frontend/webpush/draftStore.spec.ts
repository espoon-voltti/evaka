// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'fake-indexeddb/auto'
import { afterEach, describe, expect, it } from 'vitest'

import { deleteDraft, loadDraft, purgeOldDrafts, saveDraft } from './draftStore'

async function resetDb() {
  await new Promise<void>((resolve, reject) => {
    const req = indexedDB.deleteDatabase('evaka-citizen-webpush')
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
    req.onblocked = () => resolve()
  })
}

describe('draftStore', () => {
  afterEach(async () => {
    await resetDb()
  })

  it('saves and loads a draft by threadId', async () => {
    await saveDraft('thread-a', 'Hello from notification')
    const draft = await loadDraft('thread-a')
    expect(draft).not.toBeNull()
    expect(draft!.threadId).toBe('thread-a')
    expect(draft!.text).toBe('Hello from notification')
    expect(typeof draft!.savedAt).toBe('number')
  })

  it('returns null when no draft exists', async () => {
    const draft = await loadDraft('nonexistent')
    expect(draft).toBeNull()
  })

  it('overwrites an existing draft for the same threadId', async () => {
    await saveDraft('thread-a', 'first')
    await saveDraft('thread-a', 'second')
    const draft = await loadDraft('thread-a')
    expect(draft!.text).toBe('second')
  })

  it('deletes a draft', async () => {
    await saveDraft('thread-a', 'hi')
    await deleteDraft('thread-a')
    expect(await loadDraft('thread-a')).toBeNull()
  })

  it('keeps drafts for other threads when deleting one', async () => {
    await saveDraft('thread-a', 'a')
    await saveDraft('thread-b', 'b')
    await deleteDraft('thread-a')
    expect(await loadDraft('thread-a')).toBeNull()
    expect((await loadDraft('thread-b'))!.text).toBe('b')
  })

  it('purges drafts older than the TTL', async () => {
    await saveDraft('old', 'stale')
    // Manually rewrite savedAt to simulate age
    await new Promise<void>((resolve, reject) => {
      const open = indexedDB.open('evaka-citizen-webpush', 1)
      open.onsuccess = () => {
        const db = open.result
        const tx = db.transaction('drafts', 'readwrite')
        const store = tx.objectStore('drafts')
        const get = store.get('old') as IDBRequest<{
          threadId: string
          text: string
          savedAt: number
        }>
        get.onsuccess = () => {
          const value = get.result
          value.savedAt = Date.now() - 1000 * 60 * 60 * 24 * 30 // 30 days ago
          store.put(value)
        }
        tx.oncomplete = () => {
          db.close()
          resolve()
        }
        tx.onerror = () => reject(tx.error)
      }
      open.onerror = () => reject(open.error)
    })

    await saveDraft('fresh', 'fresh draft')
    await purgeOldDrafts(1000 * 60 * 60 * 24 * 7) // 7 days TTL

    expect(await loadDraft('old')).toBeNull()
    expect((await loadDraft('fresh'))!.text).toBe('fresh draft')
  })
})
