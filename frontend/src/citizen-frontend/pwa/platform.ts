// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isIOS } from 'lib-common/utils/helpers'

export type Platform = 'ios' | 'android' | 'other'

/** Picks the instructions to show, because every platform hides these settings elsewhere */
export const platform = (): Platform =>
  isIOS() ? 'ios' : /android/i.test(navigator.userAgent) ? 'android' : 'other'
