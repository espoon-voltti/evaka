// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'

import type { Translations } from 'lib-customizations/citizen'

export default function useTitle(t: Translations, title: string) {
  const effectiveTitle = title ? `${t.common.title} | ${title}` : t.common.title
  const fallbackTitle = t.common.title
  return useEffect(() => {
    document.title = effectiveTitle
    return () => {
      document.title = fallbackTitle
    }
  }, [effectiveTitle, fallbackTitle])
}
