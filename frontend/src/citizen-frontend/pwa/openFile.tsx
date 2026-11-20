// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useRef } from 'react'

import { isIOS } from 'lib-common/utils/helpers'
import { NotificationsContext } from 'lib-components/Notifications'
import { OpenFileContext, openInNewTab } from 'lib-components/utils/openFile'
import colors from 'lib-customizations/common'

import { useTranslation } from '../localization'

import { useIsRunningInstalled } from './installed'

async function openInInstalledIosApp(
  view: Window | null,
  url: string,
  loadingText: string
) {
  try {
    if (view) showLoading(view.document, loadingText)
    const res = await fetch(url)
    if (!res.ok) throw new Error(`Failed to fetch file: ${res.status}`)
    const blob = await res.blob()
    const name = fileName(res.headers.get('Content-Disposition'))
    // A blob URL is outside the app scope, so iOS shows it in a separate view that can be closed
    const objectUrl = URL.createObjectURL(
      name ? new File([blob], name, { type: blob.type }) : blob
    )
    if (view) {
      view.location.replace(objectUrl)
    } else {
      const link = document.createElement('a')
      link.href = objectUrl
      link.download = name ?? ''
      link.click()
    }
    setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
  } catch (err) {
    view?.close()
    throw err
  }
}

function fileName(contentDisposition: string | null): string | undefined {
  const encoded = contentDisposition?.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) {
    try {
      return decodeURIComponent(encoded)
    } catch {
      return undefined
    }
  }
  return contentDisposition?.match(/filename="([^"]+)"/i)?.[1]
}

function showLoading(doc: Document, text: string) {
  doc.title = 'eVaka'
  const style = doc.createElement('style')
  style.textContent = `
    body {
      margin: 0;
      background-color: ${colors.grayscale.g0};
      color: ${colors.grayscale.g100};
      font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.5;
    }
    main {
      max-width: 32rem;
      margin: 0 auto;
      padding: 3rem 1.5rem;
    }
    h1 {
      color: ${colors.main.m2};
      font-size: 1.5rem;
      margin: 0 0 2rem;
    }
  `
  const viewport = doc.createElement('meta')
  viewport.name = 'viewport'
  viewport.content = 'width=device-width, initial-scale=1'
  doc.head.append(viewport, style)
  const main = doc.createElement('main')
  const heading = doc.createElement('h1')
  heading.textContent = 'eVaka'
  const paragraph = doc.createElement('p')
  paragraph.textContent = text
  main.append(heading, paragraph)
  doc.body.append(main)
}

export const OpenFileProvider = React.memo(function OpenFileProvider({
  children
}: {
  children: React.ReactNode
}) {
  const t = useTranslation()
  const { addTimedNotification } = useContext(NotificationsContext)
  const runningInstalled = useIsRunningInstalled()
  const pending = useRef<{ view: Window | null } | null>(null)

  const openFile = useCallback(
    async (url: string) => {
      // In the installed app, iOS replaces the whole app window with a file opened from the app's own origin, leaving no way back
      if (!runningInstalled || !isIOS()) {
        openInNewTab(url)
        return true
      }
      if (pending.current && !pending.current.view?.closed) return false
      // Opened before fetching: after a slow fetch the click no longer counts as user activation and the popup would be blocked
      const view = window.open('', '_blank')
      if (view) view.opener = null
      const current = { view }
      pending.current = current
      try {
        await openInInstalledIosApp(view, url, t.common.openingFile)
        return true
      } catch (err) {
        addTimedNotification({
          children:
            err instanceof TypeError
              ? t.common.errors.networkError
              : t.common.errors.genericGetError
        })
        return false
      } finally {
        if (pending.current === current) pending.current = null
      }
    },
    [runningInstalled, addTimedNotification, t]
  )

  return (
    <OpenFileContext.Provider value={openFile}>
      {children}
    </OpenFileContext.Provider>
  )
})
