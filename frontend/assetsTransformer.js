// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')

module.exports = {
  process(_src, filename) {
    return 'module.exports = ' + JSON.stringify(path.basename(filename)) + ';'
  }
}
