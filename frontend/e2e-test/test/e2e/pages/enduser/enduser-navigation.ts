// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ClientFunction, Selector, t } from 'testcafe'
import Home from '../home'
import Applications from './applications'
import config from '../../config'

const home = new Home()

type ApplicationType = 'daycare' | 'club' | 'preschool'

export default class EnduserPage {
  readonly applications = new Applications()
  readonly mapTab = Selector('.menu-item').withText('KARTTA')
  readonly applicationsTab = Selector('.menu-item').withText('HAKEMUKSET')
  readonly decisionsTab = Selector('.menu-item').withText('PÄÄTÖKSET')
  readonly logoutText = Selector('.column.one')
    .find('p')
    .withText('The logout operation is complete')
  readonly applicationList = Selector('.application')
  readonly decisionList = Selector('.decision')
  readonly applicationTypeSelectModal = Selector('.modal-wrapper')

  async login() {
    await home.login('enduser')
  }

  isLoggedIn() {
    return home.isLoggedIn()
  }

  async logout() {
    await home.logout()
    if (config.env === 'test') {
      await t.expect(this.logoutText.visible).ok()
    } else {
      await t.expect(home.logoWrapper.exists).notOk()
    }
  }

  async navigateToApplicationsTab() {
    await t.click(this.applicationsTab)
    await t.expect(this.applications.createBtn.visible).ok()
  }

  async navigateToMap() {
    await t.click(this.mapTab)
  }

  async navigateDecisions() {
    await t.click(this.decisionsTab)
  }

  async createApplication(type: ApplicationType, childNumber = 0) {
    await this.applications.create(childNumber)
    const applicationType = new ApplicationTypeSelection()
    await applicationType.selectApplicationType(type)
    await applicationType.createApplication()
    await t.expect(Selector('#preview-and-send')).ok()
  }

  getApplicationById(applicationId: string) {
    return Selector(`[data-application-id="${applicationId}"]`).nth(0)
  }

  async editApplication(id: string) {
    const applicationElement = this.getApplicationById(id)
    await t.click(applicationElement.find('[data-qa="btn-edit-application"]'))
  }

  async assertApplicationStatus(
    applicationId: string,
    expectedStatusText: string
  ) {
    await t
      .expect(
        this.getApplicationById(applicationId).withText(expectedStatusText)
      )
      .ok()
  }

  getDecisionByApplicationId(applicationId: string) {
    return this.decisionList.filter(`[data-application-id=${applicationId}]`)
  }

  async assertDecisionStatus(
    applicationId: string,
    expectedStatusText: string
  ) {
    await t
      .expect(
        this.getDecisionByApplicationId(applicationId).withText(
          expectedStatusText
        )
      )
      .ok()
  }

  async sendDecision(applicationId: string) {
    const application = this.getApplicationById(applicationId)
    await t.click(application.find('[data-qa="btn-decisions"]'))
    await t.click(Selector('[data-qa="btn-send-decision"]'))
    await t.click(Selector('#decision-modal-ok-button'))
    await t.click(Selector('[data-qa="btn-accept"'))
  }

  async assertDownloadPDF(
    applicationId: string,
    decisionName = 'Varhaiskasvatuspäätös'
  ) {
    const application = this.getApplicationById(applicationId)
    await t.click(application.find('[data-qa="btn-decisions"]'))
    const downloadURL = await Selector(
      '[data-qa="link-open-decision-pdf"]'
    ).getAttribute('href')
    const response: PDFResponse = await getPdf(downloadURL)
    await t.expect(response.ok).eql(true)
    await t.expect(response.contentType).eql('application/pdf')
    await t.expect(response.size).gt(10000)
    await t
      .expect(response.contentDisposition)
      .eql(
        `attachment;filename=${decisionName}_Jari-Petteri_Mukkelis-Makkelis_Vetelä-Viljami_Eelis-Juhani_Karhula.pdf`
      )
  }
}

interface PDFResponse {
  size: number
  ok: boolean
  contentType: string
  contentDisposition: string
}

const getPdf = ClientFunction((url: string) =>
  fetch(url, {
    method: 'GET',
    credentials: 'include'
  }).then((res) => {
    return res.blob().then(
      (blob): PDFResponse => {
        return {
          size: blob.size,
          ok: res.ok,
          contentType: res.headers.get('Content-Type') ?? '',
          contentDisposition: res.headers.get('Content-Disposition') ?? ''
        }
      }
    )
  })
)

export class ApplicationTypeSelection {
  constructor(
    readonly btnSelectApplicationType = Selector(
      '[data-qa="btn-select-application-type"]'
    ),
    readonly daycare = Selector('#radio-daycare'),
    readonly club = Selector('#radio-club'),
    readonly preschool = Selector('#radio-preschool')
  ) {}

  async selectApplicationType(type: string) {
    switch (type) {
      case 'daycare':
        return t.click(this.daycare)
      case 'club':
        return t.click(this.club)
      case 'preschool':
        return t.click(this.preschool)
    }
  }

  async createApplication() {
    await t.click(this.btnSelectApplicationType)
  }
}
