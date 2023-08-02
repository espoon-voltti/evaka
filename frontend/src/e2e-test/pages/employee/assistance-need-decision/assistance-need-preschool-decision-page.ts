// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Checkbox, Combobox, Page, TextInput } from '../../../utils/page'

export default class AssistanceNeedPreschoolDecisionPage {
  constructor(private readonly page: Page) {}

  readonly autoSaveIndicator = this.page.findByDataQa('autosave-indicator')
  readonly status = this.page.findByDataQa('status')
  readonly decisionNumber = this.page.findByDataQa('decision-number')
  readonly typeRadioNew = this.page.findByDataQa('type-radio-NEW')
  readonly validFromInput = new TextInput(this.page.findByDataQa('valid-from'))
  readonly unitSelect = new Combobox(this.page.findByDataQa('unit-select'))
  readonly primaryGroupInput = new TextInput(
    this.page.findByDataQa('primary-group')
  )
  readonly decisionBasisInput = new TextInput(
    this.page.findByDataQa('decision-basis')
  )
  readonly basisPedagogicalReportCheckbox = new Checkbox(
    this.page.findByDataQa('basis-pedagogical-report')
  )
  readonly guardiansHeardInput = new TextInput(
    this.page.findByDataQa('guardians-heard')
  )
  readonly guardianHeardCheckbox = (n: number) =>
    new Checkbox(this.page.findAllByDataQa('guardian-heard').nth(n))
  readonly guardianDetailsInput = (n: number) =>
    new TextInput(this.page.findAllByDataQa('guardian-details').nth(n))
  readonly viewOfGuardiansInput = new TextInput(
    this.page.findByDataQa('view-of-guardians')
  )
  readonly decisionMakerSelect = new Combobox(
    this.page.findByDataQa('decision-maker-select')
  )
  readonly decisionMakerTitleInput = new TextInput(
    this.page.findByDataQa('decision-maker-title')
  )
  readonly preparer1Select = new Combobox(
    this.page.findByDataQa('preparer-1-select')
  )
  readonly preparer1TitleInput = new TextInput(
    this.page.findByDataQa('preparer-1-title')
  )
  readonly previewButton = this.page.findByDataQa('preview-button')
  readonly editButton = this.page.findByDataQa('edit-button')
  readonly sendDecisionButton = this.page.findByDataQa('send-decision')
}
