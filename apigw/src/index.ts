// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'source-map-support/register'
import { gatewayRole } from './shared/config'

if (!gatewayRole || gatewayRole === 'enduser') {
  require('./enduser')
}
if (!gatewayRole || gatewayRole === 'internal') {
  require('./internal')
}
