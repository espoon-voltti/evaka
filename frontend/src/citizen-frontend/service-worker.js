// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* global self, indexedDB */

/// <reference lib="WebWorker" />
/** @type {ServiceWorkerGlobalScope} */
const serviceWorker = self

// Minimal IndexedDB draft store — schema must stay in sync with
// `./webpush/draftStore.ts` (used by the main thread). The SW can't import
// that module directly because the vite dev middleware serves this file as a
// classic script, not an ES module. Duplicating the minimal save/delete code
// is simpler than wiring up module-mode service workers + dev path resolution.
const DRAFT_DB_NAME = 'evaka-citizen-webpush'
const DRAFT_DB_VERSION = 1
const DRAFT_STORE = 'drafts'

function openDraftDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DRAFT_DB_NAME, DRAFT_DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(DRAFT_STORE)) {
        db.createObjectStore(DRAFT_STORE, { keyPath: 'threadId' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function saveDraft(threadId, text) {
  const db = await openDraftDb()
  await new Promise((resolve, reject) => {
    const tx = db.transaction(DRAFT_STORE, 'readwrite')
    tx.objectStore(DRAFT_STORE).put({ threadId, text, savedAt: Date.now() })
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

async function deleteDraft(threadId) {
  const db = await openDraftDb()
  await new Promise((resolve, reject) => {
    const tx = db.transaction(DRAFT_STORE, 'readwrite')
    tx.objectStore(DRAFT_STORE).delete(threadId)
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

// Adds the notification-origin markers the SPA looks for on arrival:
//   - fromNotification=1: consumed by RequireAuth to append
//     reason=session-expired-open-thread when the user is not logged in.
//     Stripped from the post-login return URL.
//   - scrollTo=latest: consumed by ThreadView to scroll to the last
//     message on mount. Stripped from the URL via history.replaceState
//     after the first scroll.
// Preserves any existing query params on the input URL. Idempotent: safe
// to call on a URL that already carries the markers (set() replaces).
function notificationClickUrl(targetUrl) {
  try {
    const url = new URL(targetUrl, self.location.origin)
    url.searchParams.set('fromNotification', '1')
    url.searchParams.set('scrollTo', 'latest')
    return url.pathname + url.search + url.hash
  } catch {
    // If targetUrl is unparseable (shouldn't happen for the backend-
    // supplied URLs we see today), return it unchanged so we at least
    // open something.
    return targetUrl
  }
}

serviceWorker.addEventListener('install', (event) => {
  event.waitUntil(serviceWorker.skipWaiting())
})

serviceWorker.addEventListener('activate', (event) => {
  event.waitUntil(serviceWorker.clients.claim())
})

// No-op fetch handler. Chrome's PWA installability check only requires that
// a `fetch` listener is attached — the listener itself doesn't need to call
// `respondWith`. Leaving the handler empty lets the browser handle every
// request normally, avoiding interference with Playwright's navigation
// response tracking under e2e test runs (which causes spurious
// "response guid was not bound in the connection" failures on the next
// page.goto after the SW installs).
serviceWorker.addEventListener('fetch', () => {
  // intentionally empty
})

serviceWorker.addEventListener('push', (event) => {
  // Backend sends a JSON array of WebPushPayload entries (sealed class, tagged with
  // `type`), not a single object, so we pick the first NotificationV1 entry.
  const payload = (() => {
    try {
      const parsed = event.data ? event.data.json() : null
      const list = Array.isArray(parsed) ? parsed : parsed ? [parsed] : []
      return list.find((p) => p && p.type === 'NotificationV1') ?? list[0] ?? {}
    } catch {
      return {}
    }
  })()
  const title = payload.title ?? 'eVaka'
  const actions = []
  if (payload.replyAction) {
    actions.push({
      action: 'reply',
      type: 'text',
      title: payload.replyAction.actionLabel,
      placeholder: payload.replyAction.actionPlaceholder
    })
  }
  event.waitUntil(
    serviceWorker.registration.showNotification(title, {
      body: payload.body ?? '',
      icon: payload.iconPath ?? '/citizen/evaka-192px.png',
      // Android Chrome uses the alpha channel of the badge image to draw a
      // monochrome mask in the status bar, so the file is a transparent PNG
      // with just the "e" glyph opaque.
      badge: '/citizen/evaka-badge-72.png',
      tag: payload.tag,
      data: {
        url: payload.url ?? '/messages',
        replyAction: payload.replyAction ?? null
      },
      actions
    })
  )
})

serviceWorker.addEventListener('notificationclick', (event) => {
  const notification = event.notification
  const data = notification.data ?? {}
  const url = data.url ?? '/messages'
  const replyAction = data.replyAction

  if (event.action === 'reply' && replyAction) {
    const reply = event.reply
    if (typeof reply === 'string' && reply.trim().length > 0) {
      notification.close()
      event.waitUntil(handleInlineReply(replyAction, reply.trim()))
      return
    }
    // Platform rendered the action as a plain button (no inline text input).
    // Fall through to opening the thread with the reply textarea focused.
    notification.close()
    event.waitUntil(
      openUrl(
        notificationClickUrl(`/messages/${replyAction.threadId}?focus=reply`)
      )
    )
    return
  }

  notification.close()
  event.waitUntil(openUrl(notificationClickUrl(url)))
})

async function handleInlineReply(replyAction, content) {
  const threadId = replyAction.threadId
  // Save the draft BEFORE attempting the POST so it survives session expiry
  // and the SAML login round-trip on error.
  try {
    await saveDraft(threadId, content)
  } catch (err) {
    // If IDB is unavailable we still attempt the POST; loss of draft on
    // failure is acceptable when IDB is broken.
    console.warn('Failed to persist reply draft', err)
  }

  try {
    const response = await fetch(`/api/citizen/messages/reply-to/${threadId}`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'content-type': 'application/json',
        'x-evaka-csrf': '1'
      },
      body: JSON.stringify({
        content,
        recipientAccountIds: replyAction.recipientAccountIds
      })
    })
    if (!response.ok) {
      /** @type {any} */
      const err = new Error(`reply POST failed: ${response.status}`)
      err.status = response.status
      throw err
    }
    await deleteDraft(threadId).catch((err) =>
      console.warn('Failed to delete reply draft after successful send', {
        threadId,
        err
      })
    )
    await serviceWorker.registration.showNotification(
      replyAction.successTitle,
      {
        body: replyAction.successBody,
        icon: '/citizen/notifications/reply-success.png',
        badge: '/citizen/evaka-badge-72.png',
        tag: `msg-${threadId}`,
        data: { url: notificationClickUrl(`/messages/${threadId}`) },
        actions: []
      }
    )
  } catch (err) {
    console.warn('Reply POST failed', { threadId, err })

    /** @type {any} */
    const errAny = err
    if (errAny && errAny.status === 401) {
      // Session expired. Open the PWA straight on the login page with the
      // reason banner so the citizen can re-authenticate without a second
      // notification tap. The draft was already saved to IDB before the
      // POST, so useReplyDraftRestore repopulates the reply editor when
      // the citizen lands back on the thread.
      const returnUrl = `/messages/${threadId}?focus=reply&scrollTo=latest`
      const loginUrl =
        `/login?next=${encodeURIComponent(returnUrl)}` +
        `&reason=session-expired-reply-failed`
      try {
        const win = await serviceWorker.clients.openWindow(loginUrl)
        if (win) return
      } catch (openErr) {
        console.warn('openWindow failed for auth-expired reply', openErr)
      }
      // Fall through to the error-notification path so the user is not
      // left with a silent failure. They'll land on the same thread and
      // the draft will be restored, they just lose the reason banner.
    }

    await serviceWorker.registration.showNotification(replyAction.errorTitle, {
      body: replyAction.errorBody,
      icon: '/citizen/notifications/reply-error.png',
      badge: '/citizen/evaka-badge-72.png',
      tag: `msg-reply-error-${threadId}`,
      requireInteraction: true,
      data: {
        url: notificationClickUrl(`/messages/${threadId}?focus=reply`)
      },
      actions: []
    })
  }
}

async function openUrl(url) {
  const list = await serviceWorker.clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  })
  for (const client of list) {
    if (client.url.includes(url) && 'focus' in client) {
      return client.focus()
    }
  }
  return serviceWorker.clients.openWindow(url)
}
