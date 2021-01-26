// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ClientFunction, Selector, t } from 'testcafe'
import { ApplicationTypeSelection } from './enduser-navigation'
import ClubApplication from './club-application-form'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'

interface Child {
  childName: string
  applications: ChildApplication[]
}

interface ChildApplication {
  applicationId: string
  type: 'DAYCARE' | 'CLUB' | 'PRESCHOOL'
  isEditable: boolean
  status: string
}

export default class Applications {
  readonly createBtn = Selector('.add-application')

  async create(i: number) {
    // There is one create button per child. nth(0) is the first child's button, nth(1) second etc.
    await t.click(this.createBtn.nth(i).find('button'))
  }

  async getChildren(): Promise<Child[]> {
    await t
      .expect(
        Selector('[data-qa="children-wrapper"] > .child-container').visible
      )
      .ok()
    return ClientFunction(() => {
      const result: Child[] = []
      for (const childContainer of Array.from(
        document.querySelectorAll(
          '[data-qa="children-wrapper"] > .child-container'
        )
      )) {
        const childName =
          childContainer.querySelector('.child-name')?.textContent?.trim() ?? ''
        const applicationList = childContainer.querySelector('ul.content')
        const applications: ChildApplication[] = []
        if (applicationList) {
          for (const application of Array.from(applicationList.children)) {
            const applicationId =
              application.getAttribute('data-application-id') ?? ''
            const type = application.getAttribute(
              'data-qa-type'
            ) as ChildApplication['type']
            const isEditable = !!application.querySelector(
              '[data-qa="btn-edit-application"]'
            )
            const status =
              application
                .querySelector('[data-qa="application-status"]')
                ?.textContent?.trim() ?? ''
            applications.push({ applicationId, type, isEditable, status })
          }
        }
        result.push({ childName, applications })
      }
      return result
    })()
  }

  async getFirstEditableApplication(
    applicationType: ApplicationType,
    applicationStatus: 'LÄHETETTY' | 'LUONNOS' = 'LÄHETETTY'
  ): Promise<string> {
    const children = await this.getChildren()
    for (const child of children) {
      for (const {
        applicationId,
        isEditable,
        type,
        status
      } of child.applications) {
        if (
          isEditable &&
          type === applicationType &&
          status === applicationStatus
        ) {
          return applicationId
        }
      }
    }
    const totalApplications = children.reduce(
      (sum, child) => child.applications.length,
      0
    )
    throw new Error(
      `No editable application found. Searched ${children.length} children with a total of ${totalApplications} applications`
    )
  }

  async modifyApplicationById(applicationId: string) {
    const applicationRow: ApplicationRow = new ApplicationRow(applicationId)
    await applicationRow.modifyApplication()
  }

  async checkAndGetStatus(applicationId: string): Promise<string> {
    const applicationRow: ApplicationRow = new ApplicationRow(applicationId)
    await t.expect(applicationRow.applicationListItem.exists).ok()
    return await applicationRow.getStatus()
  }

  async verifyCreated(applicationId: string) {
    await t.expect(await this.checkAndGetStatus(applicationId)).eql('LUONNOS')
  }

  async verifySent(applicationId: string) {
    await t.expect(await this.checkAndGetStatus(applicationId)).eql('LÄHETETTY')
  }

  async createClubApplication() {
    await t.expect(this.createBtn.visible).ok()
    await this.create(0)

    // Select club as application type
    const applicationType = new ApplicationTypeSelection()
    await applicationType.selectApplicationType('CLUB')
    await applicationType.createApplication()
    const clubApplication = new ClubApplication()
    await t.expect(clubApplication.checkAndSendBtn.visible).ok()

    const applicationId = await clubApplication.getApplicationId()
    return applicationId
  }
}

export class ApplicationRow {
  constructor(
    applicationId: string,
    readonly applicationListItem = Selector(
      `[data-application-id="${applicationId}"]`
    )
  ) {}

  async getStatus() {
    // Trim whitespaces for Edge browser.
    return (
      await this.applicationListItem.find('[data-qa="application-status"]')
        .innerText
    ).trim()
  }

  async modifyApplication() {
    await t.click(
      this.applicationListItem.find('[data-qa="btn-edit-application"]')
    )
  }

  isEditable() {
    return this.applicationListItem.find('[data-qa="btn-edit-application"]')
      .exists
  }

  getApplicationType() {
    return this.applicationListItem.getAttribute('data-qa-type')
  }
}
