// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Config } from '../config'

export const emptyRedisConfig: Config['redis'] = {
  host: undefined,
  port: undefined,
  tlsServerName: undefined,
  password: undefined,
  disableSecurity: true
}
