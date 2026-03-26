// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'assert'

import type { ApplicationStatus } from 'lib-common/generated/api-types/application'
import type { ProviderType } from 'lib-common/generated/api-types/daycare'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'

import { captureTextualDownload } from '../../browser'
import { expect } from '../../playwright'
import type { Page, Element, ElementCollection } from '../../utils/page'
import {
  Checkbox,
  Combobox,
  DatePicker,
  MultiSelect,
  Select,
  TextInput,
  TreeDropdown
} from '../../utils/page'

import { ChildDocumentPage } from './documents/child-document'

export default class ReportsPage {
  constructor(private readonly page: Page) {}

  async openMissingHeadOfFamilyReport() {
    await this.page.findByDataQa('report-missing-head-of-family').click()
    return new MissingHeadOfFamilyReport(this.page)
  }

  async openApplicationsReport() {
    await this.page.findByDataQa('report-applications').click()
    return new ApplicationsReport(this.page)
  }

  async openNonSsnChildrenReport() {
    await this.page.findByDataQa('report-non-ssn-children').click()
    return new NonSsnChildrenReport(this.page)
  }

  async openPlacementGuaranteeReport() {
    await this.page.findByDataQa('report-placement-guarantee').click()
    return new PlacementGuaranteeReport(this.page)
  }

  async openPlacementSketchingReport() {
    await this.page.findByDataQa('report-placement-sketching').click()
    return new PlacementSketchingReport(this.page)
  }

  async openVoucherServiceProvidersReport() {
    await this.page.findByDataQa('report-voucher-service-providers').click()
    return new VoucherServiceProvidersReport(this.page)
  }

  async openPreschoolAbsenceReport() {
    await this.page.findByDataQa('report-preschool-absence').click()
    return new PreschoolAbsenceReport(this.page)
  }

  async openPreschoolApplicationReport() {
    await this.page.findByDataQa('report-preschool-application').click()
    return new PreschoolApplicationReport(this.page)
  }

  async openHolidayPeriodAttendanceReport() {
    await this.page.findByDataQa('report-holiday-period-attendance').click()
    return new HolidayPeriodAttendanceReport(this.page)
  }

  async openStartingPlacementsReport() {
    await this.page.findByDataQa('report-starting-placements').click()
    return new StartingPlacementsReport(this.page)
  }

  async openEndedPlacementsReport() {
    await this.page.findByDataQa('report-ended-placements').click()
    return new EndedPlacementsReport(this.page)
  }

  async openSextetReport() {
    await this.page.findByDataQa('report-sextet').click()
    return new SextetReport(this.page)
  }

  async openChildDocumentDecisionsReport() {
    await this.page.findByDataQa('report-child-document-decisions').click()
    return new ChildDocumentDecisionsReport(this.page)
  }

  async openOccupanciesReport() {
    await this.page.findByDataQa('report-occupancies').click()
    return new OccupanciesReport(this.page)
  }
}

export class MissingHeadOfFamilyReport {
  constructor(private page: Page) {}

  async toggleShowIntentionalDuplicates() {
    await new Checkbox(
      this.page.findByDataQa('show-intentional-duplicates-checkbox')
    ).toggle()
  }

  async assertRows(
    expected: {
      childName: string
      rangesWithoutHead: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('missing-head-of-family-row')
    await expect(rows).toHaveCount(expected.length)
    for (const [index, data] of expected.entries()) {
      const row = rows.nth(index)
      await expect(row.findByDataQa('child-name')).toHaveText(data.childName, {
        useInnerText: true
      })
      await expect(row.findByDataQa('ranges-without-head')).toHaveText(
        data.rangesWithoutHead,
        { useInnerText: true }
      )
    }
  }
}

export class NonSsnChildrenReport {
  #nameHeader: Element

  constructor(private page: Page) {
    this.#nameHeader = page.findByDataQa(`child-name-header`)
  }

  async changeSortOrder() {
    await this.#nameHeader.click()
  }

  async assertRows(
    expected: {
      childName: string
      dateOfBirth: string
      ophPersonOid: string
      lastSentToVarda: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('non-ssn-child-row')
    await expect(rows).toHaveCount(expected.length)
    for (const [index, data] of expected.entries()) {
      const row = rows.nth(index)
      await expect(row.findByDataQa('child-name')).toHaveText(data.childName, {
        useInnerText: true
      })
      await expect(row.findByDataQa('date-of-birth')).toHaveText(
        data.dateOfBirth,
        { useInnerText: true }
      )
      await expect(row.findByDataQa('oph-person-oid')).toHaveText(
        data.ophPersonOid,
        { useInnerText: true }
      )
      await expect(row.findByDataQa('last-sent-to-varda')).toHaveText(
        data.lastSentToVarda,
        { useInnerText: true }
      )
    }
  }
}

export class ApplicationsReport {
  #table: Element
  #areaSelector: Combobox

  constructor(private page: Page) {
    this.#table = page.findByDataQa(`report-application-table`)
    this.#areaSelector = new Combobox(page.findByDataQa('select-area'))
  }

  private async areaWithNameExists(area: string, exists = true) {
    await expect(this.#table).toBeVisible()

    const areaElem = this.#table
      .findAll(`[data-qa="care-area-name"]:has-text("${area}")`)
      .first()

    if (exists) {
      await expect(areaElem).toBeVisible()
    } else {
      await expect(areaElem).toBeHidden()
    }
  }

  async assertContainsArea(area: string) {
    await this.areaWithNameExists(area, true)
  }

  async assertDoesntContainArea(area: string) {
    await this.areaWithNameExists(area, false)
  }

  async assertContainsServiceProviders(providers: string[]) {
    await expect(this.#table).toBeVisible()

    for (const provider of providers) {
      await expect(
        this.#table
          .findAll(`[data-qa="unit-provider-type"]:has-text("${provider}")`)
          .first()
      ).toBeVisible()
    }
  }

  async selectArea(area: string) {
    await this.#areaSelector.fillAndSelectFirst(area)
  }

  async selectDateRangePickerDates(from: LocalDate, to: LocalDate) {
    const fromInput = new DatePicker(this.page.findByDataQa('datepicker-from'))
    const toInput = new DatePicker(this.page.findByDataQa('datepicker-to'))
    await fromInput.fill(from.format())
    await toInput.fill(to.format())
  }
}

export class PlacementGuaranteeReport {
  constructor(private page: Page) {}

  async selectDate(formattedDate: string) {
    const datePicker = new DatePicker(this.page.findByDataQa('date-picker'))
    await datePicker.fill(formattedDate)
  }

  async selectUnit(unitName: string) {
    const unitSelector = new Combobox(this.page.findByDataQa('unit-selector'))
    await unitSelector.fillAndSelectFirst(unitName)
  }

  async assertRows(
    expected: {
      areaName: string
      unitName: string
      childName: string
      placementPeriod: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('placement-guarantee-row')
    await expect(rows).toHaveCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)
        await expect(row.findByDataQa('area-name')).toHaveText(data.areaName, {
          useInnerText: true
        })
        await expect(row.findByDataQa('unit-name')).toHaveText(data.unitName, {
          useInnerText: true
        })
        await expect(row.findByDataQa('child-name')).toHaveText(
          data.childName,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('placement-period')).toHaveText(
          data.placementPeriod,
          { useInnerText: true }
        )
      })
    )
  }
}

export class PlacementSketchingReport {
  #applicationStatus: MultiSelect

  constructor(private page: Page) {
    this.#applicationStatus = new MultiSelect(
      page.findByDataQa('select-application-status')
    )
  }

  async assertRow(
    applicationId: string,
    requestedUnitName: string,
    childName: string,
    currentUnitName: string | null = null
  ) {
    const element = this.page.findByDataQa(`${applicationId}`)
    await expect(element).toBeVisible()

    await expect(element.find('[data-qa="requested-unit"]')).toHaveText(
      requestedUnitName,
      { useInnerText: true }
    )
    if (currentUnitName) {
      await expect(element.find('[data-qa="current-unit"]')).toHaveText(
        currentUnitName,
        { useInnerText: true }
      )
    }
    await expect(element.find('[data-qa="child-name"]')).toHaveText(childName, {
      useInnerText: true
    })
  }

  async assertNotRow(applicationId: string) {
    const element = this.page.findByDataQa(`${applicationId}`)
    await expect(element).toBeHidden()
  }

  async toggleApplicationStatus(status: ApplicationStatus) {
    await this.#applicationStatus.click() // open options menu
    await this.#applicationStatus.selectItem(status)
    await this.#applicationStatus.click() // close options menu
  }
}

export class VoucherServiceProvidersReport {
  #month: Select
  #year: Select
  #area: Select
  #downloadCsvLink: Element

  constructor(private page: Page) {
    this.#month = new Select(page.findByDataQa('select-month'))
    this.#year = new Select(page.findByDataQa('select-year'))
    this.#area = new Select(page.findByDataQa('select-area'))
    this.#downloadCsvLink = page.findByDataQa('download-csv')
  }

  async selectMonth(month: 'Tammikuu') {
    await this.#month.selectOption({ label: month.toLowerCase() })
  }

  async selectYear(year: number) {
    await this.#year.selectOption({ label: String(year) })
  }

  async selectArea(area: string) {
    await this.#area.selectOption({ label: area })
  }

  async assertRowCount(expectedChildCount: number) {
    await expect(this.page.findAll('.reportRow')).toHaveCount(
      expectedChildCount
    )
  }

  async assertRow(
    unitId: string,
    expectedChildCount: string,
    expectedMonthlySum: string
  ) {
    const row = this.page.findByDataQa(`${unitId}`)
    await expect(row).toBeVisible()
    expect(await row.find(`[data-qa="child-count"]`).text).toStrictEqual(
      expectedChildCount
    )
    expect(await row.find(`[data-qa="child-sum"]`).text).toStrictEqual(
      expectedMonthlySum
    )
  }

  async getCsvReport(): Promise<string> {
    const [download] = await Promise.all([
      this.page.waitForDownload(),
      this.#downloadCsvLink.click()
    ])
    return captureTextualDownload(download)
  }

  async openUnitReport(unitName: string) {
    await this.page.findTextExact(unitName).click()
  }
}

export class ServiceVoucherUnitReport {
  #childRows: ElementCollection

  constructor(private page: Page) {
    this.#childRows = page.findAllByDataQa('child-row')
  }

  async assertChildRowCount(expected: number) {
    await expect(this.#childRows).toHaveCount(expected)
  }

  async assertChild(
    nth: number,
    expectedName: string,
    expectedVoucherValue: number,
    expectedCoPayment: number,
    expectedRealizedAmount: number
  ) {
    await expect(this.page.findAllByDataQa('child-name').nth(nth)).toHaveText(
      expectedName,
      { useInnerText: true }
    )
    await this.page
      .findAllByDataQa('voucher-value')
      .nth(nth)
      .assertText((text) => parseFloat(text) === expectedVoucherValue)
    await this.page
      .findAllByDataQa('co-payment')
      .nth(nth)
      .assertText((text) => parseFloat(text) === expectedCoPayment)
    await this.page
      .findAllByDataQa('realized-amount')
      .nth(nth)
      .assertText((text) => parseFloat(text) === expectedRealizedAmount)
  }
}

export class AssistanceNeedsAndActionsReport {
  needsAndActionsHeader: Element
  needsAndActionsRows: ElementCollection
  childRows: ElementCollection
  careAreaSelect: Combobox
  providerTypeSelect: Combobox
  unitSelect: Combobox
  typeSelect: Combobox
  daycareAssistanceLevelSelect: MultiSelect
  preschoolAssistanceLevelSelect: MultiSelect
  assistanceActionOptionSelect: MultiSelect
  placementTypeSelect: MultiSelect
  zeroRowsCheckbox: Checkbox
  includeDecisionsCheckbox: Checkbox

  constructor(private page: Page) {
    this.needsAndActionsHeader = page.findByDataQa(
      'assistance-needs-and-actions-header'
    )
    this.needsAndActionsRows = page.findAllByDataQa(
      'assistance-needs-and-actions-row'
    )
    this.childRows = page.findAllByDataQa('child-row')
    this.careAreaSelect = new Combobox(page.findByDataQa('care-area-filter'))
    this.unitSelect = new Combobox(page.findByDataQa('unit-filter'))
    this.typeSelect = new Combobox(page.findByDataQa(`type-filter`))
    this.providerTypeSelect = new Combobox(
      this.page.findByDataQa('provider-type-filter')
    )
    this.daycareAssistanceLevelSelect = new MultiSelect(
      this.page.findByDataQa('daycare-assistance-level-filter')
    )
    this.preschoolAssistanceLevelSelect = new MultiSelect(
      this.page.findByDataQa('preschool-assistance-level-filter')
    )
    this.assistanceActionOptionSelect = new MultiSelect(
      this.page.findByDataQa('assistance-action-option-filter')
    )
    this.placementTypeSelect = new MultiSelect(
      this.page.findByDataQa('placement-type-filter')
    )
    this.zeroRowsCheckbox = new Checkbox(
      this.page.findByDataQa('zero-rows-checkbox')
    )
    this.includeDecisionsCheckbox = new Checkbox(
      this.page.findByDataQa('include-decisions-checkbox')
    )
  }

  async selectCareAreaFilter(area: string) {
    await this.careAreaSelect.fillAndSelectFirst(area)
  }

  async openUnit(unitName: string) {
    await this.page.findByDataQa(`unit-${unitName}`).click()
  }
}

export class PreschoolAbsenceReport {
  unitSelector: Combobox
  termSelector: Combobox
  groupSelector: Combobox
  filterByClosed: Checkbox

  constructor(private page: Page) {
    this.unitSelector = new Combobox(this.page.findByDataQa('unit-select'))
    this.termSelector = new Combobox(this.page.findByDataQa('term-select'))
    this.groupSelector = new Combobox(this.page.findByDataQa('group-select'))
    this.filterByClosed = new Checkbox(page.findByDataQa('filter-by-closed'))
  }

  async selectUnit(unitName: string) {
    await this.unitSelector.fillAndSelectFirst(unitName)
  }

  async selectTerm(termRange: string) {
    await this.termSelector.fillAndSelectFirst(termRange)
  }

  async assertRows(
    expected: {
      firstName: string
      lastName: string
      daycareName: string
      groupName: string
      TOTAL: string
      OTHER_ABSENCE: string
      SICKLEAVE: string
      UNKNOWN_ABSENCE: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('preschool-absence-row')
    await expect(rows).toHaveCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)
        await expect(row.findByDataQa('first-name-column')).toHaveText(
          data.firstName,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('last-name-column')).toHaveText(
          data.lastName,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('daycare-name-column')).toHaveText(
          data.daycareName,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('group-name-column')).toHaveText(
          data.groupName,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('total-column')).toHaveText(data.TOTAL, {
          useInnerText: true
        })
        await expect(row.findByDataQa('other-absence-column')).toHaveText(
          data.OTHER_ABSENCE,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('sickleave-column')).toHaveText(
          data.SICKLEAVE,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('unknown-absence-column')).toHaveText(
          data.UNKNOWN_ABSENCE,
          { useInnerText: true }
        )
      })
    )
  }
}

export class PreschoolApplicationReport {
  constructor(private page: Page) {}

  async assertNoResults() {
    const noResults = this.page.findByDataQa('no-results')
    await expect(noResults).toBeVisible()
  }

  async assertRows(expected: string[]) {
    const rows = this.page.findAllByDataQa('row')
    await expect(rows).toHaveText(expected, { useInnerText: true })
  }
}

export class HolidayPeriodAttendanceReport {
  sendButton: Element
  groupSelector: MultiSelect

  constructor(private page: Page) {
    this.sendButton = this.page.findByDataQa('send-button')
    this.groupSelector = new MultiSelect(this.page.findByDataQa('group-select'))
  }

  async selectUnit(unitName: string) {
    const unitSelector = new Combobox(this.page.findByDataQa('unit-select'))
    await unitSelector.fillAndSelectFirst(unitName)
  }

  async selectPeriod(periodRange: string) {
    const unitSelector = new Combobox(this.page.findByDataQa('period-select'))
    await unitSelector.fillAndSelectFirst(periodRange)
  }

  async assertRows(
    expected: {
      date: string
      presentChildren: string[]
      assistanceChildren: string[]
      coefficientSum: string
      staffCount: string
      absenceCount: string
      noResponseChildren: string[]
    }[]
  ) {
    const rows = this.page.findAllByDataQa('holiday-period-attendance-row')
    await expect(rows).toHaveCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)

        await expect(row.findByDataQa('date-column')).toHaveText(data.date, {
          useInnerText: true
        })

        const presentChildren = row
          .findByDataQa('present-children-column')
          .findAllByDataQa('child-name')

        await this.assertChildNames(presentChildren, data.presentChildren)
        const assistanceChildren = row
          .findByDataQa('assistance-children-column')
          .findAllByDataQa('child-name')
        await this.assertChildNames(assistanceChildren, data.assistanceChildren)

        await expect(row.findByDataQa('coefficient-sum-column')).toHaveText(
          data.coefficientSum,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('staff-count-column')).toHaveText(
          data.staffCount,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('absence-count-column')).toHaveText(
          data.absenceCount,
          { useInnerText: true }
        )

        const noResponseChildren = row
          .findByDataQa('no-response-children-column')
          .findAllByDataQa('child-name')
        await this.assertChildNames(noResponseChildren, data.noResponseChildren)
      })
    )
  }

  private async assertChildNames(children: ElementCollection, names: string[]) {
    const childCount = await children.count()
    assert(childCount === names.length)
    for (let i = 0; i < childCount; i++) {
      const child = children.nth(i)
      await expect(child).toHaveText(`${i + 1}. ${names[i]}`, {
        useInnerText: true
      })
    }
  }
}

export class ChildAttendanceReservationByChildReport {
  startDateInput: DatePicker
  endDateInput: DatePicker
  unitSelector: Combobox
  groupSelector: MultiSelect
  filterByTime: Checkbox
  filterByShiftCare: Checkbox
  filterByClosed: Checkbox
  startTimeInput: TextInput
  endTimeInput: TextInput
  searchButton: Element

  constructor(private page: Page) {
    this.startDateInput = new DatePicker(page.findByDataQa('start-date'))
    this.endDateInput = new DatePicker(page.findByDataQa('end-date'))
    this.unitSelector = new Combobox(page.findByDataQa('unit-select'))
    this.groupSelector = new MultiSelect(page.findByDataQa('group-select'))
    this.filterByTime = new Checkbox(page.findByDataQa('filter-by-time'))
    this.filterByShiftCare = new Checkbox(
      page.findByDataQa('filter-by-shift-care')
    )
    this.filterByClosed = new Checkbox(page.findByDataQa('filter-by-closed'))
    this.searchButton = page.findByDataQa('search-button')
    this.startTimeInput = new TextInput(page.findByDataQa('start-time-filter'))
    this.endTimeInput = new TextInput(page.findByDataQa('end-time-filter'))
  }

  async setDates(startDate: LocalDate, endDate: LocalDate) {
    await this.startDateInput.fill(startDate)
    await this.endDateInput.fill(endDate)
  }

  async endDate(date: LocalDate) {
    await this.endDateInput.fill(date)
  }

  async selectUnit(unitName: string) {
    await this.unitSelector.fillAndSelectFirst(unitName)
  }

  async selectGroup(groupName: string) {
    await this.groupSelector.fillAndSelectFirst(groupName)
  }

  async selectTimeFilter(startTime: string, endTime: string) {
    await this.filterByTime.check()
    await this.startTimeInput.fill(startTime)
    await this.endTimeInput.fill(endTime)
  }

  async assertRows(
    expected: {
      childName: string
      attendanceReservationStart: string | null
      attendanceReservationEnd: string | null
    }[]
  ) {
    const rows = this.page.findAllByDataQa('child-attendance-reservation-row')
    await expect(rows).toHaveCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)
        await expect(row.findByDataQa('child-name')).toHaveText(
          data.childName,
          { useInnerText: true }
        )
        if (data.attendanceReservationStart && data.attendanceReservationEnd) {
          await expect(
            row.findByDataQa('attendance-reservation-start')
          ).toHaveText(data.attendanceReservationStart, { useInnerText: true })
          await expect(
            row.findByDataQa('attendance-reservation-end')
          ).toHaveText(data.attendanceReservationEnd, { useInnerText: true })
        } else {
          await expect(row.findByDataQa('missing-reservation-text')).toHaveText(
            'Varaus puuttuu',
            { useInnerText: true }
          )
        }
      })
    )
  }
}

export class ChildDocumentsReport {
  unitSelector: MultiSelect
  templateSelector: TreeDropdown

  constructor(private page: Page) {
    this.unitSelector = new MultiSelect(page.findByDataQa('unit-select'))
    this.templateSelector = new TreeDropdown(
      page.findByDataQa('template-select')
    )
  }

  getUnitRow(id: DaycareId) {
    const row = this.page.findByDataQa(`unit-row-${id}`)
    return {
      name: row.findByDataQa('name'),
      drafts: row.findByDataQa('drafts-count'),
      prepared: row.findByDataQa('prepared-count'),
      completed: row.findByDataQa('completed-count'),
      noDocuments: row.findByDataQa('no-documents-count'),
      total: row.findByDataQa('total-count')
    }
  }

  async toggleUnitRowGroups(id: DaycareId) {
    await this.page.findByDataQa(`unit-${id}-toggle-groups`).click()
  }

  getGroupRow(id: GroupId) {
    const row = this.page.findByDataQa(`group-row-${id}`)
    return {
      name: row.findByDataQa('name'),
      drafts: row.findByDataQa('drafts-count'),
      prepared: row.findByDataQa('prepared-count'),
      completed: row.findByDataQa('completed-count'),
      noDocuments: row.findByDataQa('no-documents-count'),
      total: row.findByDataQa('total-count')
    }
  }
}

export class ChildDocumentDecisionsReport {
  childDocumentDetails: ElementCollection

  constructor(private page: Page) {
    this.childDocumentDetails = page.findAllByDataQa('child-document-details')
  }

  async clickDocument(nth: number): Promise<ChildDocumentPage> {
    const childDocument = new Promise<ChildDocumentPage>((res) => {
      this.page.onPopup((page) => res(new ChildDocumentPage(page)))
    })

    await this.childDocumentDetails.nth(nth).click()
    return childDocument
  }
}

export class OccupanciesReport {
  areaCombobox: Combobox
  unitsSelect: MultiSelect
  typeCombobox: Combobox

  constructor(private page: Page) {
    this.areaCombobox = new Combobox(page.findByDataQa('filter-area'))
    this.unitsSelect = new MultiSelect(page.findByDataQa('filter-units'))
    this.typeCombobox = new Combobox(page.findByDataQa('filter-type'))
  }

  async assertReportDateColumns(expected: LocalDate[]) {
    const columns = this.page.findAllByDataQa('table-header-date')
    await expect(columns).toHaveText(
      expected.map((date) => date.format('dd.MM.')),
      { useInnerText: true }
    )
  }

  async assertReportUnitNameRows(expected: string[]) {
    const rows = this.page.findAllByDataQa('table-body-row-unit')
    const names = rows.findAllByDataQa('table-body-row-unit-name')
    await expect(names).toHaveText(expected, { useInnerText: true })
  }
}

export class StartingPlacementsReport {
  constructor(private page: Page) {}

  async assertRows(
    expected: {
      childName: string
      areaName: string
      unitName: string
      placementStart: LocalDate
    }[]
  ) {
    const rows = this.page.findAllByDataQa('report-row')
    await expect(rows).toHaveCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)
        await expect(row.findByDataQa('child-name')).toHaveText(
          data.childName,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('area-name')).toHaveText(data.areaName, {
          useInnerText: true
        })
        await expect(row.findByDataQa('unit-name')).toHaveText(data.unitName, {
          useInnerText: true
        })
        await expect(row.findByDataQa('placement-start-date')).toHaveText(
          data.placementStart.format(),
          { useInnerText: true }
        )
      })
    )
  }
}

export class EndedPlacementsReport {
  constructor(private page: Page) {}

  async assertRows(
    expected: {
      childName: string
      childDateOfBirth: LocalDate
      areaName: string
      unitName: string
      placementEnd: LocalDate
      nextPlacementStart: LocalDate
      nextPlacementUnitName: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('report-row')
    await expect(rows).toHaveCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)
        await expect(row.findByDataQa('child-name')).toHaveText(
          data.childName,
          { useInnerText: true }
        )
        await expect(row.findByDataQa('child-date-of-birth')).toHaveText(
          data.childDateOfBirth.format(),
          { useInnerText: true }
        )
        await expect(row.findByDataQa('area-name')).toHaveText(data.areaName, {
          useInnerText: true
        })
        await expect(row.findByDataQa('unit-name')).toHaveText(data.unitName, {
          useInnerText: true
        })
        await expect(row.findByDataQa('placement-end-date')).toHaveText(
          data.placementEnd.format(),
          { useInnerText: true }
        )
        await expect(row.findByDataQa('next-placement-start-date')).toHaveText(
          data.nextPlacementStart.format(),
          { useInnerText: true }
        )
        await expect(row.findByDataQa('next-placement-unit-name')).toHaveText(
          data.nextPlacementUnitName,
          { useInnerText: true }
        )
      })
    )
  }
}

export class SextetReport {
  constructor(private page: Page) {}

  async selectYear(year: number) {
    const combobox = new Combobox(this.page.findByDataQa('filter-year'))
    await combobox.fillAndSelectFirst(`${year}`)
  }

  async selectPlacementType(placementType: PlacementType) {
    const combobox = new Combobox(
      this.page.findByDataQa('filter-placement-type')
    )
    await combobox.click()
    await combobox.fillAndSelectItem(
      '',
      `filter-placement-type-${placementType}`
    )
  }

  async toggleUnitProviderType(unitProviderType: ProviderType) {
    const checkbox = new Checkbox(
      this.page.findByDataQa(`filter-unit-provider-type-${unitProviderType}`)
    )
    await checkbox.click()
  }

  async assertRows(expected: string[]) {
    const rows = this.page.findAllByDataQa('data-rows')
    await expect(rows).toHaveText(expected, { useInnerText: true })
  }

  async assertSum(expected: number) {
    const sum = this.page.findByDataQa('data-sum')
    await expect(sum).toHaveText(`${expected}`, { useInnerText: true })
  }
}
