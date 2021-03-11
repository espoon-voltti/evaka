// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'
import { Translations } from './localization'

export default function useTitle(
  t: Translations,
  title: string,
  deps: any[] = [] // eslint-disable-line @typescript-eslint/no-explicit-any
) {
  return useEffect(() => {
    document.title = title ? `${t.common.title} | ${title}` : t.common.title
    return () => void (document.title = t.common.title)
  }, deps)
}
