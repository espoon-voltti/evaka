// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Checkbox,
  Combobox,
  Page,
  TextInput,
  Element
} from '../../../utils/page'

export default class AssistanceNeedPreschoolDecisionPage {
  autoSaveIndicator: Element
  status: Element
  decisionNumber: Element
  typeRadioNew: Element
  validFromInput: TextInput
  unitSelect: Combobox
  primaryGroupInput: TextInput
  decisionBasisInput: TextInput
  basisPedagogicalReportCheckbox: Checkbox
  guardiansHeardInput: TextInput
  viewOfGuardiansInput: TextInput
  decisionMakerSelect: Combobox
  decisionMakerTitleInput: TextInput
  preparer1Select: Combobox
  preparer1TitleInput: TextInput
  previewButton: Element
  editButton: Element
  sendDecisionButton: Element
  constructor(private readonly page: Page) {
    this.autoSaveIndicator = page.findByDataQa('autosave-indicator')
    this.status = page.findByDataQa('status')
    this.decisionNumber = page.findByDataQa('decision-number')
    this.typeRadioNew = page.findByDataQa('type-radio-NEW')
    this.validFromInput = new TextInput(page.findByDataQa('valid-from'))
    this.unitSelect = new Combobox(page.findByDataQa('unit-select'))
    this.primaryGroupInput = new TextInput(page.findByDataQa('primary-group'))
    this.decisionBasisInput = new TextInput(page.findByDataQa('decision-basis'))
    this.basisPedagogicalReportCheckbox = new Checkbox(
      page.findByDataQa('basis-pedagogical-report')
    )
    this.guardiansHeardInput = new TextInput(
      page.findByDataQa('guardians-heard')
    )
    this.viewOfGuardiansInput = new TextInput(
      page.findByDataQa('view-of-guardians')
    )
    this.decisionMakerSelect = new Combobox(
      page.findByDataQa('decision-maker-select')
    )
    this.decisionMakerTitleInput = new TextInput(
      page.findByDataQa('decision-maker-title')
    )
    this.preparer1Select = new Combobox(page.findByDataQa('preparer-1-select'))
    this.preparer1TitleInput = new TextInput(
      page.findByDataQa('preparer-1-title')
    )
    this.previewButton = page.findByDataQa('preview-button')
    this.editButton = page.findByDataQa('edit-button')
    this.sendDecisionButton = page.findByDataQa('send-decision')
  }

  readonly guardianHeardCheckbox = (n: number) =>
    new Checkbox(this.page.findAllByDataQa('guardian-heard').nth(n))
  readonly guardianDetailsInput = (n: number) =>
    new TextInput(this.page.findAllByDataQa('guardian-details').nth(n))
}
