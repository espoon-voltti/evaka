// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../e2e-test/config'
import type { DevPerson } from '../../e2e-test/generated/api-types'

import { citizenLogin } from './login'
import { navigate } from './navigate'
import { storeTestConfig } from './test-config'

// A static page on the app's origin: storage and the session cookie can be set
// there without the app running its own auth checks concurrently
const setupPageUrl = `${config.enduserUrl}/offline.html`

export async function openAsInstalledApp(person: DevPerson, path: string) {
  await navigate(setupPageUrl)
  await storeTestConfig()
  await citizenLogin(person, path)
  await $('html[data-standalone]').waitForExist({
    timeoutMsg:
      'html[data-standalone] did not appear: is the forceStandalone test config supported by the app?'
  })
}

export async function waitForCalendar() {
  await $('[data-qa="calendar-page"][data-isloading="false"]').waitForExist()
}
