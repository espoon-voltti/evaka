// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import type HelsinkiDateTime from '../../helsinki-date-time'

/**
* Generated from fi.espoo.evaka.webpush.WebPushSubscription
*/
export interface WebPushSubscription {
  authSecret: number[]
  ecdhKey: number[]
  endpoint: string
  expires: HelsinkiDateTime | null
}
