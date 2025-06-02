// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'

import { useTranslation } from '../state/i18n'

interface TitleOptions {
  hideDefault?: boolean
  preventUpdate?: boolean
}

export function useTitle(title?: string, options: TitleOptions = {}): void {
  const { i18n } = useTranslation()
  const { hideDefault, preventUpdate } = options

  useEffect(() => {
    if (preventUpdate) return

    document.title = title
      ? `${title}${hideDefault ? '' : ` - ${i18n.titles.defaultTitle}`}`
      : i18n.titles.defaultTitle
  }, [hideDefault, i18n, preventUpdate, title])
}
