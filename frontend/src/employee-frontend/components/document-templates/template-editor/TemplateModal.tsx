// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import DateRange from 'lib-common/date-range'
import { openEndedLocalDateRange } from 'lib-common/form/fields'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type {
  ExportedDocumentTemplate,
  DocumentType
} from 'lib-common/generated/api-types/document'
import { documentTypes } from 'lib-common/generated/api-types/document'
import type { DocumentTemplateId } from 'lib-common/generated/api-types/shared'
import { uiLanguages } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import { useMutationResult } from 'lib-common/query'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import {
  featureFlags,
  placementTypes as placementTypeValues
} from 'lib-customizations/employee'

import { useTranslation } from '../../../state/i18n'
import { documentTemplateForm } from '../forms'
import {
  createDocumentTemplateMutation,
  duplicateDocumentTemplateMutation,
  importDocumentTemplateMutation
} from '../queries'

export type TemplateModalMode =
  | { type: 'new' }
  | { type: 'duplicate'; from: DocumentTemplateId }
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
        .filter(
          (type) =>
            !type.startsWith('MIGRATED_') &&
            (featureFlags.citizenChildDocumentTypes ||
              type !== 'CITIZEN_BASIC') &&
            (featureFlags.decisionChildDocumentTypes ||
              type !== 'OTHER_DECISION')
        )
        .map((option) => ({
          domValue: option,
          value: option,
          label: i18n.documentTemplates.documentTypes[option]
        })),
    [i18n.documentTemplates]
  )

  const getLanguageOptions = useCallback(
    (type: DocumentType) =>
      uiLanguages
        .filter((option) => type === 'CITIZEN_BASIC' || option !== 'EN')
        .map((option) => ({
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
              options: getLanguageOptions(mode.data.type)
            },
            confidential: mode.data.confidentiality !== null,
            confidentialityDurationYears:
              mode.data.confidentiality?.durationYears?.toString() ?? '',
            confidentialityBasis: mode.data.confidentiality?.basis ?? '',
            legalBasis: mode.data.legalBasis,
            validity: openEndedLocalDateRange.fromRange(
              DateRange.parseJson(mode.data.validity)
            ),
            processDefinitionNumber: mode.data.processDefinitionNumber ?? '',
            archiveDurationMonths:
              mode.data.archiveDurationMonths?.toString() ?? '120',
            archiveExternally: mode.data.archiveExternally
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
              options: getLanguageOptions('PEDAGOGICAL_ASSESSMENT')
            },
            confidential: true,
            confidentialityDurationYears: '100',
            confidentialityBasis: '',
            legalBasis: '',
            validity: openEndedLocalDateRange.empty(),
            processDefinitionNumber: '',
            archiveDurationMonths: '120',
            archiveExternally: false
          },
    {
      ...i18n.validationErrors
    },
    {
      onUpdate: (_, next, form) => {
        const shape = form.shape()
        const type = shape.type.validate(next.type)
        if (type.isValid) {
          const options = getLanguageOptions(type.value)
          return {
            ...next,
            language: {
              options,
              domValue: options.some(
                (o) => o.domValue === next.language.domValue
              )
                ? next.language.domValue
                : 'FI'
            }
          }
        } else {
          return next
        }
      }
    }
  )

  const {
    name,
    type,
    placementTypes,
    language,
    confidential,
    confidentialityDurationYears,
    confidentialityBasis,
    legalBasis,
    validity,
    processDefinitionNumber,
    archiveDurationMonths,
    archiveExternally
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
      {confidential.state && (
        <>
          <Gap />
          <Label>
            {i18n.documentTemplates.templateModal.confidentialityDuration}
          </Label>
          <InputFieldF
            data-qa="confidentiality-duration-years"
            bind={confidentialityDurationYears}
            type="number"
            hideErrorsBeforeTouched
          />{' '}
          <Gap />
          <Label>
            {i18n.documentTemplates.templateModal.confidentialityBasis}
          </Label>
          <InputFieldF
            data-qa="confidentiality-basis"
            bind={confidentialityBasis}
            hideErrorsBeforeTouched
          />
        </>
      )}
      <Gap />
      <ExpandingInfo
        info={i18n.documentTemplates.templateModal.processDefinitionNumberInfo}
        width="auto"
      >
        <Label>
          {i18n.documentTemplates.templateModal.processDefinitionNumber}
        </Label>
      </ExpandingInfo>
      <InputFieldF
        bind={processDefinitionNumber}
        hideErrorsBeforeTouched
        data-qa="process-definition-number"
      />
      {((processDefinitionNumber.state?.trim()?.length ?? 0) > 0 ||
        archiveExternally.state) && (
        <>
          <Gap />
          <Label>
            {i18n.documentTemplates.templateModal.archiveDurationMonths}
          </Label>
          <InputFieldF
            data-qa="archive-duration-months"
            bind={archiveDurationMonths}
            type="number"
            hideErrorsBeforeTouched
          />
        </>
      )}
      <Gap />
      <CheckboxF
        bind={archiveExternally}
        label={i18n.documentTemplates.templateModal.archiveExternally}
        data-qa="archive-externally-checkbox"
      />
    </AsyncFormModal>
  )
})
