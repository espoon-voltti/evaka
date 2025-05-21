// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import path from 'node:path'

export default {
  process(_src, filename) {
    return 'module.exports = ' + JSON.stringify(path.basename(filename)) + ';'
  }
}
