// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  boolean,
  openEndedLocalDateRange,
  string
} from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  oneOf,
  required,
  transformed,
  union,
  validated,
  value
} from 'lib-common/form/form'
import {
  type StateOf,
  ValidationError,
  ValidationSuccess
} from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import type {
  DocumentTemplateBasicsRequest,
  DocumentTemplateContent,
  ChildDocumentType,
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { UiLanguage } from 'lib-common/generated/api-types/shared'
import { getDocumentCategory } from 'lib-components/document-templates/documents'
import CheckboxGroupQuestionDescriptor from 'lib-components/document-templates/question-descriptors/CheckboxGroupQuestionDescriptor'
import CheckboxQuestionDescriptor from 'lib-components/document-templates/question-descriptors/CheckboxQuestionDescriptor'
import DateQuestionDescriptor from 'lib-components/document-templates/question-descriptors/DateQuestionDescriptor'
import GroupedTextFieldsQuestionDescriptor from 'lib-components/document-templates/question-descriptors/GroupedTextFieldsQuestionDescriptor'
import RadioButtonGroupQuestionDescriptor from 'lib-components/document-templates/question-descriptors/RadioButtonGroupQuestionDescriptor'
import StaticTextDisplayQuestionDescriptor from 'lib-components/document-templates/question-descriptors/StaticTextDisplayQuestionDescriptor'
import TextQuestionDescriptor from 'lib-components/document-templates/question-descriptors/TextQuestionDescriptor'

const caseManagementRequired: ChildDocumentType[] = [
  'VASU',
  'HOJKS',
  'LEOPS',
  'OTHER_DECISION',
  'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION',
  'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'
]

export const documentTemplateForm = transformed(
  object({
    name: validated(string(), nonBlank),
    type: required(oneOf<ChildDocumentType>()),
    placementTypes: validated(array(value<PlacementType>()), (arr) =>
      arr.length === 0 ? 'required' : undefined
    ),
    language: required(oneOf<UiLanguage>()),
    confidential: boolean(),
    confidentialityDurationYears: required(value<string>()),
    confidentialityBasis: required(value<string>()),
    legalBasis: string(),
    validity: required(openEndedLocalDateRange()),
    processDefinitionNumber: required(value<string>()),
    archiveDurationMonths: required(value<string>()),
    archiveExternally: boolean(),
    endDecisionWhenUnitChanges: boolean()
  }),
  (value) => {
    const caseManaged = value.processDefinitionNumber.trim().length > 0
    if (caseManaged) {
      const archiveDurationMonths = parseInt(value.archiveDurationMonths)
      if (isNaN(archiveDurationMonths) || archiveDurationMonths < 1) {
        return ValidationError.field('archiveDurationMonths', 'integerFormat')
      }
    } else if (caseManagementRequired.includes(value.type)) {
      return ValidationError.field('processDefinitionNumber', 'required')
    }

    if (value.archiveExternally) {
      if (value.processDefinitionNumber.trim().length === 0) {
        return ValidationError.field('processDefinitionNumber', 'required')
      }

      if (value.archiveDurationMonths.trim().length === 0) {
        return ValidationError.field('archiveDurationMonths', 'required')
      }

      const archiveDurationMonths = parseInt(value.archiveDurationMonths)
      if (isNaN(archiveDurationMonths) || archiveDurationMonths < 1) {
        return ValidationError.field('archiveDurationMonths', 'integerFormat')
      }
    }

    const confidential = value.confidential
    if (confidential) {
      const confidentialityDurationYears = parseInt(
        value.confidentialityDurationYears
      )
      if (
        isNaN(confidentialityDurationYears) ||
        confidentialityDurationYears < 1
      ) {
        return ValidationError.field(
          'confidentialityDurationYears',
          'integerFormat'
        )
      }
      if (value.confidentialityBasis.trim().length === 0) {
        return ValidationError.field('confidentialityBasis', 'required')
      }
    }

    const output: DocumentTemplateBasicsRequest = {
      ...value,

      confidentiality: confidential
        ? {
            durationYears: parseInt(value.confidentialityDurationYears),
            basis: value.confidentialityBasis.trim()
          }
        : null,
      ...(value.archiveExternally
        ? {
            templateType: 'ARCHIVED_EXTERNALLY',
            processDefinitionNumber: value.processDefinitionNumber.trim(),
            archiveDurationMonths: parseInt(value.archiveDurationMonths)
          }
        : {
            templateType: 'REGULAR',
            processDefinitionNumber: caseManaged
              ? value.processDefinitionNumber.trim()
              : null,
            archiveDurationMonths: caseManaged
              ? parseInt(value.archiveDurationMonths)
              : null
          }),
      endDecisionWhenUnitChanges:
        getDocumentCategory(value.type) === 'decision'
          ? value.endDecisionWhenUnitChanges
          : null
    }
    return ValidationSuccess.of(output)
  }
)

export const templateQuestionForm = mapped(
  union({
    TEXT: TextQuestionDescriptor.template.form,
    CHECKBOX: CheckboxQuestionDescriptor.template.form,
    CHECKBOX_GROUP: CheckboxGroupQuestionDescriptor.template.form,
    RADIO_BUTTON_GROUP: RadioButtonGroupQuestionDescriptor.template.form,
    STATIC_TEXT_DISPLAY: StaticTextDisplayQuestionDescriptor.template.form,
    DATE: DateQuestionDescriptor.template.form,
    GROUPED_TEXT_FIELDS: GroupedTextFieldsQuestionDescriptor.template.form
  }),
  (output): Question => {
    switch (output.branch) {
      case 'TEXT':
        return {
          type: output.branch,
          ...output.value
        }
      case 'CHECKBOX':
        return {
          type: output.branch,
          ...output.value
        }
      case 'CHECKBOX_GROUP':
        return {
          type: output.branch,
          ...output.value
        }
      case 'RADIO_BUTTON_GROUP':
        return {
          type: output.branch,
          ...output.value
        }
      case 'STATIC_TEXT_DISPLAY':
        return {
          type: output.branch,
          ...output.value
        }
      case 'DATE':
        return {
          type: output.branch,
          ...output.value
        }
      case 'GROUPED_TEXT_FIELDS':
        return {
          type: output.branch,
          ...output.value
        }
    }
  }
)

export const templateSectionForm = object({
  id: validated(string(), nonBlank),
  label: validated(string(), nonBlank),
  questions: array(templateQuestionForm),
  infoText: string()
})

export const templateContentForm = object({
  sections: array(templateSectionForm)
})

export const getTemplateQuestionInitialState = (question: Question) => {
  switch (question.type) {
    case 'TEXT':
      return TextQuestionDescriptor.template.getInitialState(question)
    case 'CHECKBOX':
      return CheckboxQuestionDescriptor.template.getInitialState(question)
    case 'CHECKBOX_GROUP':
      return CheckboxGroupQuestionDescriptor.template.getInitialState(question)
    case 'RADIO_BUTTON_GROUP':
      return RadioButtonGroupQuestionDescriptor.template.getInitialState(
        question
      )
    case 'STATIC_TEXT_DISPLAY':
      return StaticTextDisplayQuestionDescriptor.template.getInitialState(
        question
      )
    case 'DATE':
      return DateQuestionDescriptor.template.getInitialState(question)
    case 'GROUPED_TEXT_FIELDS':
      return GroupedTextFieldsQuestionDescriptor.template.getInitialState(
        question
      )
  }
}

export const getTemplateQuestionInitialStateByType = (type: QuestionType) => {
  switch (type) {
    case 'TEXT':
      return TextQuestionDescriptor.template.getInitialState()
    case 'CHECKBOX':
      return CheckboxQuestionDescriptor.template.getInitialState()
    case 'CHECKBOX_GROUP':
      return CheckboxGroupQuestionDescriptor.template.getInitialState()
    case 'RADIO_BUTTON_GROUP':
      return RadioButtonGroupQuestionDescriptor.template.getInitialState()
    case 'STATIC_TEXT_DISPLAY':
      return StaticTextDisplayQuestionDescriptor.template.getInitialState()
    case 'DATE':
      return DateQuestionDescriptor.template.getInitialState()
    case 'GROUPED_TEXT_FIELDS':
      return GroupedTextFieldsQuestionDescriptor.template.getInitialState()
  }
}

export const getTemplateFormInitialState = (
  template: DocumentTemplateContent
): StateOf<typeof templateContentForm> => ({
  sections: template.sections.map((section) => ({
    id: section.id,
    label: section.label,
    questions: section.questions.map(getTemplateQuestionInitialState),
    infoText: section.infoText
  }))
})
