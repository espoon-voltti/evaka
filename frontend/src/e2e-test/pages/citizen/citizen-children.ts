// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import type {
  Page,
  ElementCollection,
  Element,
  EnvType
} from '../../utils/page'
import { AsyncButton, Modal, TextInput } from '../../utils/page'

import { CitizenNewAbsenceApplicationPage } from './citizen-new-absence-application'

export class CitizenChildPage {
  #placements: ElementCollection
  #terminatedPlacements: ElementCollection
  createServiceApplicationButton: Element
  openApplicationInfoBox: Element

  constructor(
    private readonly page: Page,
    private readonly env: EnvType = 'desktop'
  ) {
    this.#placements = page.findAllByDataQa('placement')
    this.#terminatedPlacements = page.findAllByDataQa('terminated-placement')
    this.createServiceApplicationButton = page.findByDataQa(
      'create-service-application'
    )
    this.openApplicationInfoBox = page.findByDataQa('open-application-info-box')
  }

  async assertChildNameIsShown(name: string) {
    await this.page.findByDataQa('child-name').assertTextEquals(name)
  }

  async goBack() {
    await this.page.findText('Palaa').click()
  }

  async openCollapsible(
    collapsible:
      | 'service-need-and-daily-service-time'
      | 'termination'
      | 'pedagogical-documents'
      | 'child-documents'
      | 'absence-applications'
  ) {
    await this.page.findByDataQa(`collapsible-${collapsible}`).click()
  }

  async closeCollapsible() {
    if (this.env === 'mobile') {
      await this.page.findByDataQa('return-collapsible').click()
    }
  }

  async assertServiceNeedTable(
    data: {
      dateRange: string
      description: string
      unit: string
    }[]
  ) {
    if (data.length > 0) {
      await this.page
        .findByDataQa(`service-need-table-${this.env}`)
        .waitUntilVisible()
      const rows = this.page.findAllByDataQa(
        `service-need-table-row-${this.env}`
      )
      await rows.assertCount(data.length)
      await Promise.all(
        data.map(async (expected, index) => {
          const row = rows.nth(index)
          await row
            .findByDataQa('service-need-date-range')
            .assertTextEquals(expected.dateRange)
          await row
            .findByDataQa('service-need-description')
            .assertTextEquals(expected.description)
          await row
            .findByDataQa('service-need-unit')
            .assertTextEquals(expected.unit)
        })
      )
    } else {
      await this.page
        .findByDataQa(`service-need-table-${this.env}`)
        .waitUntilHidden()
    }
  }

  async assertDailyServiceTimeTable(
    data: { dateRange: string; description: string }[]
  ) {
    if (data.length > 0) {
      await this.page
        .findByDataQa(`daily-service-time-table-${this.env}`)
        .waitUntilVisible()
      const rows = this.page.findAllByDataQa(
        `daily-service-time-table-row-${this.env}`
      )
      await rows.assertCount(data.length)
      await Promise.all(
        data.map(async (expected, index) => {
          const row = rows.nth(index)
          await row
            .findByDataQa('daily-service-time-date-range')
            .assertTextEquals(expected.dateRange)
          await row
            .findByDataQa('daily-service-time-description')
            .assertTextEquals(expected.description)
        })
      )
    } else {
      await this.page
        .findByDataQa(`daily-service-time-table-${this.env}`)
        .waitUntilHidden()
    }
  }

  async assertTerminatablePlacementCount(count: number) {
    await this.#placements.assertCount(count)
  }

  async assertNonTerminatablePlacementCount(count: number) {
    await this.page
      .findAllByDataQa('non-terminatable-placement')
      .assertCount(count)
  }

  async assertTerminatedPlacementCount(count: number) {
    await this.#terminatedPlacements.assertCount(count)
  }

  getTerminatedPlacements(): Promise<string[]> {
    return this.#terminatedPlacements.allTexts()
  }

  async togglePlacement(label: string) {
    await this.page.findText(label).click()
  }

  async fillTerminationDate(date: LocalDate, nth = 0) {
    await new TextInput(
      this.page.findAllByDataQa('termination-date').nth(nth)
    ).fill(date.format())
  }

  async submitTermination(nth = 0) {
    await this.page.findAll('text=Irtisano paikka').nth(nth).click()
    const modalOkButton = new AsyncButton(this.page.findByDataQa('modal-okBtn'))
    await modalOkButton.click()
    await modalOkButton.waitUntilHidden()
  }

  getTerminatablePlacements(): Promise<string[]> {
    return this.#placements.allTexts()
  }

  getNonTerminatablePlacements(): Promise<string[]> {
    return this.page.findAllByDataQa('non-terminatable-placement').allTexts()
  }

  getToggledPlacements(): Promise<string[]> {
    return this.#placements.evaluateAll((elems) =>
      elems
        .filter((e) => !!e.querySelector('input:checked'))
        .map((e) => e.textContent ?? '')
    )
  }

  childDocumentRow = (documentId: UUID) =>
    this.page.find(`tr[data-qa="child-document-${documentId}"]`)

  childDocumentLink = (documentId: UUID) =>
    this.childDocumentRow(documentId).findByDataQa('child-document-link')

  async assertAbsenceApplications(
    expected: {
      range: string
      status: string
      description: string
    }[]
  ) {
    const table = this.page.findByDataQa('absence-applications-table')
    const rows = table.findAllByDataQa('absence-application-row')
    await rows.assertTextsEqual(
      expected.map(
        (e) => `${e.range}\n${e.status}\nPoissaolon syy\n${e.description}\nPeru`
      )
    )
  }

  async newAbsenceApplicationPage() {
    await this.page.findByDataQa('create-absence-application').click()
    return new CitizenNewAbsenceApplicationPage(this.page)
  }

  async deleteAbsenceApplication(index: number) {
    const table = this.page.findByDataQa('absence-applications-table')
    const rows = table.findAllByDataQa('absence-application-row')
    const row = rows.nth(index)
    await row.findByDataQa('delete-absence-application').click()
    const modal = new Modal(
      this.page.findByDataQa('delete-absence-application-modal')
    )
    await modal.submit()
    await modal.waitUntilHidden()
  }

  assertServiceApplicationsCount = (n: number) =>
    this.page.findAllByDataQa('service-application-row').assertCount(n)
  serviceApplicationRow = (n: number) =>
    this.page.findAllByDataQa('service-application-row').nth(n)
  serviceApplicationSentDate = (n: number) =>
    this.serviceApplicationRow(n).findByDataQa('application-sent-date')
  serviceApplicationStartDate = (n: number) =>
    this.serviceApplicationRow(n).findByDataQa('application-start-date')
  serviceApplicationServiceNeed = (n: number) =>
    this.serviceApplicationRow(n).findByDataQa(
      'application-service-need-description'
    )
  serviceApplicationStatus = (n: number) =>
    this.serviceApplicationRow(n).findByDataQa('application-status')
  serviceApplicationCancelButton = (n: number) =>
    this.serviceApplicationRow(n).findByDataQa('cancel-application')

  async assertServiceApplicationDetails(
    n: number,
    additionalInfo: string,
    status: string,
    rejectedReason: string | null
  ) {
    {
      await this.serviceApplicationRow(n).findByDataQa('open-details').click()
      const modal = this.page.findByDataQa('service-application-modal')

      await modal
        .findByDataQa('additional-info')
        .assertTextEquals(additionalInfo)
      await modal
        .findByDataQa('decision-status')
        .assertText((text) => text.startsWith(status))

      if (rejectedReason) {
        await modal
          .findByDataQa('rejected-reason')
          .assertTextEquals(rejectedReason)
      } else {
        await modal.findByDataQa('rejected-reason').waitUntilHidden()
      }

      await modal.findByDataQa('close-btn').click()
    }
  }
}
