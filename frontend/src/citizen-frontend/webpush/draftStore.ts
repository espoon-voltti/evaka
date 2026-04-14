// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const DB_NAME = 'evaka-citizen-webpush'
const DB_VERSION = 1
const STORE = 'drafts'

export interface ReplyDraft {
  threadId: string
  text: string
  savedAt: number
}

function getIdb(): IDBFactory {
  // Works in both window and ServiceWorkerGlobalScope contexts
  const idb = (globalThis as unknown as { indexedDB?: IDBFactory }).indexedDB
  if (!idb) {
    throw new Error('IndexedDB is not available in this context')
  }
  return idb
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = getIdb().open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'threadId' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

function run<T>(
  mode: IDBTransactionMode,
  op: (store: IDBObjectStore) => IDBRequest<T>
): Promise<T | null> {
  return openDb().then(
    (db) =>
      new Promise<T | null>((resolve, reject) => {
        const tx = db.transaction(STORE, mode)
        const store = tx.objectStore(STORE)
        const req = op(store)
        let result: T | null = null
        req.onsuccess = () => {
          result = req.result as T
        }
        tx.oncomplete = () => {
          db.close()
          resolve(result)
        }
        tx.onerror = () => {
          db.close()
          reject(tx.error)
        }
      })
  )
}

export async function saveDraft(threadId: string, text: string): Promise<void> {
  const entry: ReplyDraft = { threadId, text, savedAt: Date.now() }
  await run('readwrite', (store) => store.put(entry))
}

export async function loadDraft(threadId: string): Promise<ReplyDraft | null> {
  const result = await run<ReplyDraft | undefined>('readonly', (store) =>
    store.get(threadId)
  )
  return result ?? null
}

export async function deleteDraft(threadId: string): Promise<void> {
  await run('readwrite', (store) => store.delete(threadId))
}

export async function purgeOldDrafts(maxAgeMs: number): Promise<void> {
  const cutoff = Date.now() - maxAgeMs
  const db = await openDb()
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    const store = tx.objectStore(STORE)
    const cursorReq = store.openCursor()
    cursorReq.onsuccess = () => {
      const cursor = cursorReq.result
      if (!cursor) return
      const value = cursor.value as ReplyDraft
      if (value.savedAt < cutoff) {
        cursor.delete()
      }
      cursor.continue()
    }
    cursorReq.onerror = () => reject(cursorReq.error)
    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => {
      db.close()
      reject(tx.error)
    }
  })
}
