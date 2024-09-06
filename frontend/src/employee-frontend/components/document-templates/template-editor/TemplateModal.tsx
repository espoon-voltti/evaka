// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import DateRange from 'lib-common/date-range'
import {
  boolean,
  openEndedLocalDateRange,
  string
} from 'lib-common/form/fields'
import {
  array,
  object,
  oneOf,
  required,
  transformed,
  validated,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import {
  DocumentTemplateBasicsRequest,
  DocumentType,
  documentTypes,
  ExportedDocumentTemplate
} from 'lib-common/generated/api-types/document'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import {
  OfficialLanguage,
  officialLanguages
} from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { placementTypes as placementTypeValues } from 'lib-customizations/employee'

import { useTranslation } from '../../../state/i18n'
import {
  createDocumentTemplateMutation,
  duplicateDocumentTemplateMutation,
  importDocumentTemplateMutation
} from '../queries'

export const documentTemplateForm = transformed(
  object({
    name: validated(string(), nonBlank),
    type: required(oneOf<DocumentType>()),
    placementTypes: validated(array(value<PlacementType>()), (arr) =>
      arr.length === 0 ? 'required' : undefined
    ),
    language: required(oneOf<OfficialLanguage>()),
    confidential: boolean(),
    legalBasis: string(),
    validity: required(openEndedLocalDateRange()),
    processDefinitionNumber: required(value<string>()),
    archiveDurationMonths: required(value<string>())
  }),
  (value) => {
    const archived = value.processDefinitionNumber.trim().length > 0
    if (archived) {
      const archiveDurationMonths = parseInt(value.archiveDurationMonths)
      if (isNaN(archiveDurationMonths) || archiveDurationMonths < 1) {
        return ValidationError.field('archiveDurationMonths', 'integerFormat')
      }
      const output: DocumentTemplateBasicsRequest = {
        ...value,
        processDefinitionNumber: value.processDefinitionNumber.trim(),
        archiveDurationMonths: archiveDurationMonths
      }
      return ValidationSuccess.of(output)
    } else {
      const output: DocumentTemplateBasicsRequest = {
        ...value,
        processDefinitionNumber: null,
        archiveDurationMonths: null
      }
      return ValidationSuccess.of(output)
    }
  }
)

export type TemplateModalMode =
  | { type: 'new' }
  | { type: 'duplicate'; from: UUID }
  | { type: 'import'; data: JsonOf<ExportedDocumentTemplate> }

interface Props {
  onClose: () => void
  mode: TemplateModalMode
}

export default React.memo(function TemplateModal({ onClose, mode }: Props) {
  const { i18n, lang } = useTranslation()

  const { mutateAsync: createDocumentTemplate } = useMutationResult(
    createDocumentTemplateMutation
  )

  const { mutateAsync: importDocumentTemplate } = useMutationResult(
    importDocumentTemplateMutation
  )

  const { mutateAsync: duplicateDocumentTemplate } = useMutationResult(
    duplicateDocumentTemplateMutation
  )

  const typeOptions = useMemo(
    () =>
      documentTypes
        .filter((type) => !type.startsWith('MIGRATED_'))
        .map((option) => ({
          domValue: option,
          value: option,
          label: i18n.documentTemplates.documentTypes[option]
        })),
    [i18n.documentTemplates]
  )

  const languageOptions = useMemo(
    () =>
      officialLanguages.map((option) => ({
        domValue: option,
        value: option,
        label: i18n.documentTemplates.languages[option]
      })),
    [i18n.documentTemplates]
  )

  const form = useForm(
    documentTemplateForm,
    () =>
      mode.type === 'import'
        ? {
            name: mode.data.name,
            type: {
              domValue: mode.data.type,
              options: typeOptions
            },
            placementTypes: mode.data.placementTypes,
            language: {
              domValue: mode.data.language,
              options: languageOptions
            },
            confidential: mode.data.confidential,
            legalBasis: mode.data.legalBasis,
            validity: openEndedLocalDateRange.fromRange(
              DateRange.parseJson(mode.data.validity)
            ),
            processDefinitionNumber: mode.data.processDefinitionNumber ?? '',
            archiveDurationMonths:
              mode.data.archiveDurationMonths?.toString() ?? '120'
          }
        : {
            name: '',
            type: {
              domValue: 'PEDAGOGICAL_ASSESSMENT',
              options: typeOptions
            },
            placementTypes: [],
            language: {
              domValue: 'FI',
              options: languageOptions
            },
            confidential: true,
            legalBasis: '',
            validity: openEndedLocalDateRange.empty(),
            processDefinitionNumber: '',
            archiveDurationMonths: '120'
          },
    {
      ...i18n.validationErrors
    }
  )

  const {
    name,
    type,
    placementTypes,
    language,
    confidential,
    legalBasis,
    validity,
    processDefinitionNumber,
    archiveDurationMonths
  } = useFormFields(form)

  return (
    <AsyncFormModal
      data-qa="template-modal"
      title={
        mode.type === 'import'
          ? i18n.documentTemplates.templatesPage.import
          : i18n.documentTemplates.templateModal.title
      }
      resolveAction={() => {
        if (mode.type === 'duplicate') {
          return duplicateDocumentTemplate({
            templateId: mode.from,
            body: form.value()
          })
        } else if (mode.type === 'import') {
          const value = form.value()
          return importDocumentTemplate({
            body: {
              ...value,
              validity: value.validity,
              content: mode.data.content
            }
          })
        } else {
          return createDocumentTemplate({ body: form.value() })
        }
      }}
      onSuccess={onClose}
      resolveLabel={i18n.common.confirm}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
    >
      <Label>{i18n.documentTemplates.templateModal.name}</Label>
      <InputFieldF bind={name} hideErrorsBeforeTouched data-qa="name-input" />
      <Gap />
      <Label>{i18n.documentTemplates.templateModal.validity}</Label>
      <DateRangePickerF bind={validity} locale={lang} />
      <Gap />
      <Label>{i18n.documentTemplates.templateModal.type}</Label>
      <SelectF bind={type} data-qa="type-select" />
      <Gap />
      <Label>{i18n.documentTemplates.templateModal.placementTypes}</Label>
      <MultiSelect
        value={placementTypes.state}
        options={placementTypeValues}
        onChange={placementTypes.set}
        getOptionId={(pt) => pt}
        getOptionLabel={(pt) => i18n.placement.type[pt]}
        placeholder={i18n.common.select}
        data-qa="placement-types-select"
      />
      <Gap />
      <Label>{i18n.documentTemplates.templateModal.language}</Label>
      <SelectF bind={language} />
      <Gap />
      <Label>{i18n.documentTemplates.templateModal.legalBasis}</Label>
      <InputFieldF bind={legalBasis} hideErrorsBeforeTouched />
      <Gap />
      <CheckboxF
        bind={confidential}
        label={i18n.documentTemplates.templateModal.confidential}
      />
      <Gap />
      <ExpandingInfo
        info={i18n.documentTemplates.templateModal.processDefinitionNumberInfo}
        width="auto"
      >
        <Label>
          {i18n.documentTemplates.templateModal.processDefinitionNumber}
        </Label>
      </ExpandingInfo>
      <InputFieldF bind={processDefinitionNumber} hideErrorsBeforeTouched />
      {processDefinitionNumber.value().trim().length > 0 && (
        <>
          <Gap />
          <Label>
            {i18n.documentTemplates.templateModal.archiveDurationMonths}
          </Label>
          <InputFieldF
            bind={archiveDurationMonths}
            type="number"
            hideErrorsBeforeTouched
          />
        </>
      )}
    </AsyncFormModal>
  )
})
