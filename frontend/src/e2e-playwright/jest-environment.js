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
    if (!this.global.evaka) return
    const {
      captureScreenshots,
      saveVideos,
      deleteTemporaryVideos,
      promises
    } = this.global.evaka

    if (event.name === 'test_fn_failure') {
      /** @type {string} **/
      const parentName = event.test.parent.name.replace(/\W/g, '-')
      /** @type {string} **/
      const specName = event.test.name.replace(/\W/g, '-')

      const namePrefix = `${parentName}_${specName}`
      await captureScreenshots(namePrefix)
      promises.push(saveVideos(namePrefix))
      promises.push(deleteTemporaryVideos(namePrefix))
    } else if (event.name === 'test_fn_success') {
      promises.push(deleteTemporaryVideos())
    }
  }
}

module.exports = PlaywrightEnvironment
