// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createContext, useContext } from 'react'

export type OpenFile = (url: string) => Promise<boolean>

export function openInNewTab(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

export const OpenFileContext = createContext<OpenFile>((url) => {
  openInNewTab(url)
  return Promise.resolve(true)
})

export const useOpenFile = (): OpenFile => useContext(OpenFileContext)
