// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  Fixture,
  testAdult,
  testAdult2,
  testChild
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const waitForPreferredLanguageSaved = (
  page: Page,
  language: 'FI' | 'SV' | 'EN'
) =>
  page.page.waitForResponse(
    (response) =>
      response.url().endsWith('/citizen/personal-data/preferred-ui-language') &&
      response.request().method() === 'PUT' &&
      response.ok() &&
      (response.request().postDataJSON() as { preferredUiLanguage: string })
        .preferredUiLanguage === language
  )

test.describe('Citizen page', () => {
  let page: Page
  let header: CitizenHeader

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await Fixture.family({ guardian: testAdult, children: [testChild] }).save()

    page = evaka
    await enduserLogin(page, testAdult, '/')
    header = new CitizenHeader(page)
  })

  test('UI language can be changed', async () => {
    await header.selectLanguage('fi')
    await header.assertSubNavMenuHasText('Valikko')
    await header.assertDOMLangAttrib('fi')

    await header.selectLanguage('sv')
    await header.assertSubNavMenuHasText('Meny')
    await header.assertDOMLangAttrib('sv')

    await header.selectLanguage('en')
    await header.assertSubNavMenuHasText('Menu')
    await header.assertDOMLangAttrib('en')
  })

  test('the chosen UI language is used when logging in from another browser', async ({
    newEvakaPage
  }) => {
    const languageSaved = waitForPreferredLanguageSaved(page, 'SV')
    await header.selectLanguage('sv')
    await languageSaved

    const otherBrowser = await newEvakaPage()
    await enduserLogin(otherBrowser, testAdult, '/')

    const otherHeader = new CitizenHeader(otherBrowser)
    await otherHeader.assertDOMLangAttrib('sv')
    await otherHeader.assertSubNavMenuHasText('Meny')
  })

  test('a citizen with no stored UI language keeps the one used in the browser', async ({
    newEvakaPage
  }) => {
    // testAdult2 has never chosen a language, so the one already in use must win
    await testAdult2.saveAdult({ updateMockVtjWithDependants: [] })

    // a browser of its own, so that testAdult cannot interfere
    const browser = await newEvakaPage()
    await browser.goto(config.enduserLoginUrl)
    await new CitizenHeader(browser).selectLanguage('sv')

    const languageSaved = waitForPreferredLanguageSaved(browser, 'SV')
    await enduserLogin(browser, testAdult2, '/')
    await languageSaved
    await new CitizenHeader(browser).assertDOMLangAttrib('sv')

    const otherBrowser = await newEvakaPage()
    await enduserLogin(otherBrowser, testAdult2, '/')
    await new CitizenHeader(otherBrowser).assertDOMLangAttrib('sv')
  })

  test('the language in the URL replaces the one used earlier in the browser', async ({
    newEvakaPage
  }) => {
    const browser = await newEvakaPage()
    await browser.goto(config.enduserLoginUrl)
    await new CitizenHeader(browser).selectLanguage('fi')

    await browser.goto(`${config.enduserLoginUrl}?lang=sv`)
    await new CitizenHeader(browser).assertDOMLangAttrib('sv')

    // the browser keeps using the URL language after the parameter is gone
    await browser.goto(config.enduserLoginUrl)
    await new CitizenHeader(browser).assertDOMLangAttrib('sv')
  })

  test('the UI language chosen by a citizen wins over the language in the URL', async () => {
    const languageSaved = waitForPreferredLanguageSaved(page, 'FI')
    await header.selectLanguage('fi')
    await languageSaved

    await page.goto(`${config.enduserUrl}/?lang=sv`)
    await header.assertSubNavMenuHasText('Valikko')
  })
})
