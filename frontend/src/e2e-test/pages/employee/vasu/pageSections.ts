// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { TextInput, Element } from '../../../utils/page'

class SimpleTextAreaSection extends Element {
  protected readonly textareas = this.findAll('[data-qa="text-question-input"]')
  protected readonly values = this.findAll('[data-qa="value-or-no-record"]')
}

export class BasicInfoSection extends SimpleTextAreaSection {
  get childName() {
    return this.findByDataQa('vasu-basic-info-child-name').text
  }

  get childDateOfBirth() {
    return this.findByDataQa('vasu-basic-info-child-dob').text
  }

  placement(nth: number) {
    return this.findAllByDataQa('vasu-basic-info-placement').nth(nth).text
  }

  guardian(nth: number) {
    return this.findAllByDataQa('vasu-basic-info-guardian').nth(nth).text
  }

  additionalContactInfoInput = new TextInput(this.textareas.nth(0))

  get additionalContactInfo() {
    return this.values.nth(0).text
  }
}

export class AuthoringSection extends SimpleTextAreaSection {
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

  childPOVInput = new TextInput(this.textareas.nth(4))
  guardianPOVInput = new TextInput(this.textareas.nth(5))

  readonly #primaryValue = this.findAll(
    '[data-qa="multi-field-question"] [data-qa="value-or-no-record"]'
  ).first()

  readonly #otherFieldsValues = this.findAll(
    '[data-qa="value-or-no-record"]'
  ).nth(1)

  get primaryValue() {
    return this.#primaryValue.text
  }

  get otherFieldsCount() {
    return this.#otherFieldsRows.count()
  }

  get otherValues() {
    return this.#otherFieldsValues.text
  }

  get childPOV() {
    return this.values.nth(2).text
  }

  get guardianPOV() {
    return this.values.nth(3).text
  }
}

export class CooperationSection extends SimpleTextAreaSection {
  collaboratorsInput = new TextInput(this.textareas.nth(0))
  methodsOfCooperationInput = new TextInput(this.textareas.nth(1))

  get collaborators() {
    return this.values.nth(0).text
  }

  get methodsOfCooperation() {
    return this.values.nth(1).text
  }
}

export class VasuGoalsSection extends SimpleTextAreaSection {
  goalsRealizationInput = new TextInput(this.textareas.nth(0))
  specialNeedsEstimationInput = new TextInput(this.textareas.nth(1))
  otherObservationsInput = new TextInput(this.textareas.nth(2))

  get goalsRealization() {
    return this.values.nth(0).text
  }

  get specialNeedsEstimation() {
    return this.values.nth(1).text
  }

  get otherObservations() {
    return this.values.nth(2).text
  }
}

export class GoalsSection extends SimpleTextAreaSection {
  childsStrengthsInput = new TextInput(this.textareas.nth(0))
  languageViewsInput = new TextInput(this.textareas.nth(1))
  pedagogicalSupportInput = new TextInput(this.textareas.nth(2))
  structuralSupportInput = new TextInput(this.textareas.nth(3))
  therapeuticSupportInput = new TextInput(this.textareas.nth(4))
  staffGoalsInput = new TextInput(this.textareas.nth(5))
  actionsInput = new TextInput(this.textareas.nth(6))
  otherInput = new TextInput(this.textareas.nth(7))

  get childsStrengths() {
    return this.values.nth(0).text
  }

  get languageViews() {
    return this.values.nth(1).text
  }

  get pedagogicalSupport() {
    return this.values.nth(2).text
  }

  get structuralSupport() {
    return this.values.nth(3).text
  }

  get therapeuticSupport() {
    return this.values.nth(4).text
  }

  get staffGoals() {
    return this.values.nth(5).text
  }

  get actions() {
    return this.values.nth(6).text
  }

  get supportLevel() {
    return this.findByDataQa('value-or-no-record-5.6').text
  }

  get other() {
    return this.values.nth(7).text
  }

  supportLevelOptions = (key: string) =>
    this.findByDataQa(`multi-select-question-option-${key}`)
  supportLevelOptionRangeStart = (key: string) =>
    new TextInput(
      this.findByDataQa(`multi-select-question-option-${key}-start-date`)
    )
  supportLevelOptionRangeEnd = (key: string) =>
    new TextInput(
      this.findByDataQa(`multi-select-question-option-${key}-end-date`)
    )
}

export class OtherSection extends SimpleTextAreaSection {
  otherInput = new TextInput(this.textareas.nth(0))

  get other() {
    return this.values.nth(0).text
  }
}

export class OtherDocsAndPlansSection extends SimpleTextAreaSection {
  otherDocsInput = new TextInput(this.textareas.nth(0))

  get otherDocs() {
    return this.values.nth(0).text
  }
}

export class InfoSharedToSection extends Element {
  recipientsOptions = (key: string) =>
    this.find(`[data-qa="multi-select-question-option-${key}"]`)
  otherInput = new TextInput(this.find('[data-qa="text-question-input"]'))

  recipients = this.find(`[data-qa="value-or-no-record-8.1"]`)
  other = this.find(`[data-qa="value-or-no-record"]`)
}

export class DiscussionSection extends SimpleTextAreaSection {
  dateInput = new TextInput(this.find('[data-qa="date-question-picker"]'))
  presentInput = new TextInput(this.textareas.nth(0))
  collaborationAndOpinionInput = new TextInput(this.textareas.nth(1))

  get date() {
    return this.values.nth(0).text
  }

  get present() {
    return this.values.nth(1).text
  }

  get collaborationAndOpinion() {
    return this.values.nth(2).text
  }
}

export class EvaluationSection extends SimpleTextAreaSection {
  descriptionInput = new TextInput(this.textareas.nth(0))

  get description() {
    return this.values.nth(0).text
  }
}
