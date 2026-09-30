// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { parseUrlWithOrigin } from 'lib-common/utils/parse-url-with-origin'

export function validatedNextPath(
  unvalidatedNextPath: string | null,
  base: { origin: string } = window.location
): string {
  const url = unvalidatedNextPath
    ? parseUrlWithOrigin(base, unvalidatedNextPath)
    : undefined
  return url && !url.pathname.startsWith('//')
    ? url.pathname + url.search + url.hash
    : '/'
}
