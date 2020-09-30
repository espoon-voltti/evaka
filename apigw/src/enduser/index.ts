// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import app from './app'
import { logInfo } from '../shared/logging'
import { httpPort } from '../shared/config'

app.listen(httpPort.enduser, () =>
  logInfo(`Evaka Application API Gateway listening on port ${httpPort.enduser}`)
)
