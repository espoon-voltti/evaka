// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Collapsible, RawElement } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class ChildInformationPage {
  constructor(private readonly page: Page) {}

  readonly feeAlterationsCollapsible = new Collapsible(
    this.page,
    '[data-qa="fee-alteration-collapsible"]'
  )
  readonly guardiansCollapsible = new Collapsible(
    this.page,
    '[data-qa="person-guardians-collapsible"]'
  )
  readonly placementsCollapsible = new Collapsible(
    this.page,
    '[data-qa="child-placements-collapsible"]'
  )
  readonly assistanceCollapsible = new Collapsible(
    this.page,
    '[data-qa="assistance-collapsible"]'
  )
  readonly backupCareCollapsible = new Collapsible(
    this.page,
    '[data-qa="backup-cares-collapsible"]'
  )
  readonly familyContactsCollapsible = new Collapsible(
    this.page,
    '[data-qa="family-contacts-collapsible"]'
  )
  readonly childApplicationsCollapsible = new Collapsible(
    this.page,
    '[data-qa="applications-collapsible"]'
  )
  readonly messageBlocklistCollapsible = new Collapsible(
    this.page,
    '[data-qa="child-message-blocklist-collapsible"]'
  )
  readonly pageWrapper = new RawElement(this.page, '.child-information-wrapper')

  async childCollapsiblesVisible(params: {
    feeAlterations: boolean
    guardiansAndParents: boolean
    placements: boolean
    assistance: boolean
    backupCare: boolean
    familyContacts: boolean
    childApplications: boolean
    messageBlocklist: boolean
  }) {
    await this.pageWrapper.waitUntilVisible()
    expect(await this.feeAlterationsCollapsible.visible).toBe(
      params.feeAlterations
    )
    expect(await this.guardiansCollapsible.visible).toBe(
      params.guardiansAndParents
    )
    expect(await this.placementsCollapsible.visible).toBe(params.placements)
    expect(await this.assistanceCollapsible.visible).toBe(params.assistance)
    expect(await this.backupCareCollapsible.visible).toBe(params.backupCare)
    expect(await this.familyContactsCollapsible.visible).toBe(
      params.familyContacts
    )
    expect(await this.childApplicationsCollapsible.visible).toBe(
      params.childApplications
    )
    expect(await this.messageBlocklistCollapsible.visible).toBe(
      params.messageBlocklist
    )
  }

  async addParentToBlockList(parentId: string) {
    await this.messageBlocklistCollapsible.open()
    await this.messageBlocklistCollapsible
      .find(`[data-qa="recipient-${parentId}"] [data-qa="blocklist-checkbox"]`)
      .click()
  }
}
