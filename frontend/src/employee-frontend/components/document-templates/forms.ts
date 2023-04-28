import { string } from 'lib-common/form/fields'
import { array, object, union, validated } from 'lib-common/form/form'
import { BoundForm } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonEmpty } from 'lib-common/form/validators'
import {
  DocumentTemplateContent,
  Question
} from 'lib-common/generated/api-types/document'

const nonEmptyString = validated(string(), nonEmpty)

export const textQuestionForm = object({
  id: nonEmptyString,
  label: nonEmptyString
})

export const multiselectQuestionForm = object({
  id: nonEmptyString,
  label: nonEmptyString,
  options: array(nonEmptyString)
})

export const questionForm = union({
  TEXT: textQuestionForm,
  MULTISELECT: multiselectQuestionForm
})

export const sectionForm = object({
  id: nonEmptyString,
  label: nonEmptyString,
  questions: array(questionForm)
})

export const templateContentForm = object({
  sections: array(sectionForm)
})

export const mapQuestionToForm = (
  q: Question
): StateOf<typeof questionForm> => {
  switch (q.type) {
    case 'TEXT':
      return {
        branch: 'TEXT' as const,
        state: {
          id: q.id,
          label: q.label
        }
      }
    case 'MULTISELECT':
      return {
        branch: 'MULTISELECT' as const,
        state: {
          id: q.id,
          label: q.label,
          options: q.options
        }
      }
  }
}

export const mapTextQuestionFromForm = (
  q: StateOf<typeof textQuestionForm>
): Question.TextQuestion => {
  return {
    type: 'TEXT',
    ...q
  }
}

export const mapMultiselectQuestionFromForm = (
  q: StateOf<typeof multiselectQuestionForm>
): Question.MultiselectQuestion => {
  return {
    type: 'MULTISELECT',
    ...q
  }
}

export const mapQuestionFromForm = (
  q: StateOf<typeof questionForm>
): Question => {
  switch (q.branch) {
    case 'TEXT':
      return mapTextQuestionFromForm(q.state)
    case 'MULTISELECT':
      return mapMultiselectQuestionFromForm(q.state)
  }
}

export const mapDocumentContentFromForm = (
  form: BoundForm<typeof templateContentForm>
): DocumentTemplateContent => ({
  sections: form.state.sections.map((section) => ({
    id: section.id,
    label: section.label,
    questions: section.questions.map(mapQuestionFromForm)
  }))
})
