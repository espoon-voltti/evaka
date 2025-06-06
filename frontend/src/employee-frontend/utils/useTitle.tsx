// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'

import type { Result } from 'lib-common/api'

import { useTranslation } from '../state/i18n'

interface TitleOptions {
  hideDefault?: boolean
}

export function useTitle(
  title: string | Result<string>,
  options: TitleOptions = {}
): void {
  const { i18n } = useTranslation()
  const { hideDefault } = options

  useEffect(() => {
    const titleText =
      typeof title === 'string'
        ? title
        : title.isSuccess
          ? title.value
          : undefined

    if (titleText === undefined) return

    document.title = title
      ? `${titleText}${hideDefault ? '' : ` - ${i18n.titles.defaultTitle}`}`
      : i18n.titles.defaultTitle
  }, [hideDefault, i18n, title])
}
