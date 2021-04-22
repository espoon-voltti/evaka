// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElement } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class ChildInformationPage {
  constructor(private readonly page: Page) {}

  readonly feeAlterationsCollapsible = new RawElement(
    this.page,
    '[data-qa="fee-alteration-collapsible"]'
  )
  readonly guardiansCollapsible = new RawElement(
    this.page,
    '[data-qa="person-guardians-collapsible"]'
  )
  readonly fridgeParentsCollapsible = new RawElement(
    this.page,
    '[data-qa="fridge-parents-collapsible"]'
  )
  readonly placementsCollapsible = new RawElement(
    this.page,
    '[data-qa="child-placements-collapsible"]'
  )
  readonly serviceNeedCollapsible = new RawElement(
    this.page,
    '[data-qa="service-need-collapsible"]'
  )
  readonly assistanceCollapsible = new RawElement(
    this.page,
    '[data-qa="assistance-collapsible"]'
  )
  readonly backupCareCollapsible = new RawElement(
    this.page,
    '[data-qa="backup-cares-collapsible"]'
  )
  readonly familyContactsCollapsible = new RawElement(
    this.page,
    '[data-qa="family-contacts-collapsible"]'
  )
  readonly childApplicationsCollapsible = new RawElement(
    this.page,
    '[data-qa="applications-collapsible"]'
  )
  readonly messageBlocklistCollapsible = new RawElement(
    this.page,
    '[data-qa="child-message-blocklist-collapsible"]'
  )
  readonly backupPickupCollapsible = new RawElement(
    this.page,
    '[data-qa="backup-pickups-collapsible"]'
  )
  readonly pageWrapper = new RawElement(this.page, '.child-information-wrapper')

  async childCollapsiblesVisible(params: {
    feeAlterations: boolean
    guardians: boolean
    fridgeParents: boolean
    placements: boolean
    serviceNeed: boolean
    assistance: boolean
    backupCare: boolean
    familyContacts: boolean
    childApplications: boolean
    messageBlocklist: boolean
    backupPickup: boolean
  }) {
    await this.pageWrapper.waitUntilVisible()
    expect(await this.feeAlterationsCollapsible.visible).toBe(
      params.feeAlterations
    )
    expect(await this.guardiansCollapsible.visible).toBe(params.guardians)
    expect(await this.fridgeParentsCollapsible.visible).toBe(
      params.fridgeParents
    )
    expect(await this.placementsCollapsible.visible).toBe(params.placements)
    expect(await this.serviceNeedCollapsible.visible).toBe(params.serviceNeed)
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
    expect(await this.backupPickupCollapsible.visible).toBe(params.backupPickup)
  }
}
