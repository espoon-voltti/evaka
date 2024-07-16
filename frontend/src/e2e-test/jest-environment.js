// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const NodeEnvironment = require('jest-environment-node').default

class PlaywrightEnvironment extends NodeEnvironment {
  async handleTestEvent(event) {
    if (!this.global.evaka) return
    const { captureScreenshots, saveTraces, promises } = this.global.evaka

    if (event.name === 'test_fn_failure') {
      /** @type {string} **/
      const parentName = event.test.parent.name.replace(/\W/g, '-')
      /** @type {string} **/
      const specName = event.test.name.replace(/\W/g, '-')

      const namePrefix = `${parentName}_${specName}`
      promises.push(saveTraces(namePrefix))
      promises.push(captureScreenshots(namePrefix))
    }
  }
}

module.exports = PlaywrightEnvironment
