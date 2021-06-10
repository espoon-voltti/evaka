// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable
  @typescript-eslint/no-unsafe-member-access,
  @typescript-eslint/no-unsafe-assignment,
  @typescript-eslint/no-unsafe-call,
  @typescript-eslint/no-var-requires,
  no-undef
*/
const NodeEnvironment = require('jest-environment-node')

class PlaywrightEnvironment extends NodeEnvironment {
  async handleTestEvent(event, _state) {
    if (event.name === 'test_fn_failure') {
      /** @type {string} **/
      const parentName = event.test.parent.name.replace(/\W/g, '-')
      /** @type {string} **/
      const specName = event.test.name.replace(/\W/g, '-')

      const namePrefix = `${parentName}_${specName}`
      await this.global.evaka?.takeScreenshots(namePrefix)
    }
  }
}

module.exports = PlaywrightEnvironment
