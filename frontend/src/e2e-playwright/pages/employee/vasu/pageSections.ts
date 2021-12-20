// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { TextInput, Element } from '../../../utils/page'

export class AuthorsSection extends Element {
  readonly #primaryInputs = this.findAll(
    '[data-qa="multi-field-question"] input'
  )
  primaryFirstNameInput = new TextInput(this.#primaryInputs.nth(0))
  primaryLastNameInput = new TextInput(this.#primaryInputs.nth(1))
  primaryTitleInput = new TextInput(this.#primaryInputs.nth(2))
  primaryPhoneNumberInput = new TextInput(this.#primaryInputs.nth(3))

  readonly #otherFieldsRows = this.findAll(
    '[data-qa="multi-field-list-question"] [data-qa="field-row"]'
  )
  readonly otherFieldsInputs = (ix: number) =>
    this.#otherFieldsRows.nth(ix).findAll('input')
  otherFirstNameInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(0))
  otherLastNameInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(1))
  otherTitleInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(2))
  otherPhoneNumberInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(3))

  readonly #primaryValue = this.findAll(
    '[data-qa="multi-field-question"] [data-qa="value-or-no-record"]'
  ).first()

  readonly #otherFieldsValues = this.findAll(
    '[data-qa="value-or-no-record"]'
  ).nth(1)

  get primaryValue(): Promise<string> {
    return this.#primaryValue.innerText
  }

  get otherFieldsCount(): Promise<number> {
    return this.#otherFieldsRows.count()
  }

  get otherValues(): Promise<string> {
    return this.#otherFieldsValues.innerText
  }
}

class SimpleTextAreaSection extends Element {
  protected readonly textareas = this.findAll('[data-qa="text-question-input"]')
  protected readonly values = this.findAll('[data-qa="value-or-no-record"]')
}

export class ConsiderationsSection extends SimpleTextAreaSection {
  childsViewInput = new TextInput(this.textareas.nth(0))
  guardiansViewInput = new TextInput(this.textareas.nth(1))

  childsView = this.values.nth(0).innerText
  guardiansView = this.values.nth(1).innerText
}

export class PreviousVasuGoalsSection extends SimpleTextAreaSection {
  goalsRealizedInput = new TextInput(this.textareas.nth(0))
  otherObservationsInput = new TextInput(this.textareas.nth(1))

  goalsRealized = this.values.nth(0).innerText
  otherObservations = this.values.nth(1).innerText
}

export class GoalsSection extends SimpleTextAreaSection {
  childsStrengthsInput = new TextInput(this.textareas.nth(0))
  goalsForTeachersInput = new TextInput(this.textareas.nth(1))
  otherInput = new TextInput(this.textareas.nth(2))

  childsStrengths = this.values.nth(0).innerText
  goalsForTeachers = this.values.nth(1).innerText
  other = this.values.nth(2).innerText
}

export class WellnessSupportSection extends SimpleTextAreaSection {
  wellnessInput = new TextInput(this.textareas.nth(0))
  wellness = this.values.nth(0).innerText
}

export class OtherDocsAndPlansSection extends SimpleTextAreaSection {
  otherDocsInput = new TextInput(this.textareas.nth(0))
  otherDocs = this.values.nth(0).innerText
}

export class InfoSharedToSection extends Element {
  readonly #values = this.findAll('[data-qa="value-or-no-record"]')

  recipientsOptions = this.findAll('[data-qa="multi-select-question-option"]')
  otherInput = new TextInput(this.find('[data-qa="text-question-input"]'))

  recipients = this.#values.nth(0).innerText
  other = this.#values.nth(1).innerText
}

export class AdditionalInfoSection extends SimpleTextAreaSection {
  infoInput = new TextInput(this.textareas.nth(0))
  info = this.values.nth(0).innerText
}
