// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPen } from 'Icons'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { swapElements } from 'lib-common/array'
import { openEndedLocalDateRange } from 'lib-common/form/fields'
import {
  useForm,
  useFormElems,
  useFormField,
  useFormFields
} from 'lib-common/form/hooks'
import {
  DocumentTemplate,
  documentTypes
} from 'lib-common/generated/api-types/document'
import { officialLanguages } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import Checkbox, { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../../state/i18n'
import LabelValueList from '../../common/LabelValueList'
import {
  forceUnpublishDocumentTemplateMutation,
  publishDocumentTemplateMutation,
  updateDocumentTemplateBasicsMutation,
  updateDocumentTemplateContentMutation
} from '../queries'
import { getTemplateFormInitialState, templateContentForm } from '../templates'

import { documentTemplateForm } from './TemplateModal'
import TemplateSectionModal from './TemplateSectionModal'
import TemplateSectionView from './TemplateSectionView'

interface Props {
  template: DocumentTemplate
  readOnly: boolean
}

export default React.memo(function TemplateContentEditor({
  template,
  readOnly
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const [creatingSection, setCreatingSection] = useState(false)
  const [readyToPublish, setReadyToPublish] = useState(false)

  const { mutateAsync: updateDocumentTemplateContent } = useMutationResult(
    updateDocumentTemplateContentMutation
  )
  const { mutateAsync: publishDocumentTemplate } = useMutationResult(
    publishDocumentTemplateMutation
  )

  const form = useForm(
    templateContentForm,
    () => getTemplateFormInitialState(template.content),
    {
      ...i18n.validationErrors
    }
  )
  const sections = useFormField(form, 'sections')
  const sectionElems = useFormElems(sections)

  return (
    <div>
      <ContentArea opaque>
        <BasicsSection template={template} editingAllowed={!readOnly} />
      </ContentArea>
      <Gap />
      <ContentArea opaque>
        <H1>{template.name}</H1>
        <H2>
          Essi Esimerkkiläinen (
          {LocalDate.todayInHelsinkiTz().subYears(5).format()})
        </H2>
        <FixedSpaceColumn spacing="L">
          {sectionElems.map((section, index) => (
            <TemplateSectionView
              key={section.state.id}
              bind={section}
              onMoveUp={() =>
                sections.update((old) => swapElements(old, index, index - 1))
              }
              onMoveDown={() =>
                sections.update((old) => swapElements(old, index, index + 1))
              }
              onDelete={() =>
                sections.update((old) => [
                  ...old.slice(0, index),
                  ...old.slice(index + 1)
                ])
              }
              first={index === 0}
              last={index === sectionElems.length - 1}
              readOnly={readOnly}
            />
          ))}
        </FixedSpaceColumn>

        {!readOnly && (
          <AddButtonRow
            text={i18n.documentTemplates.templateEditor.addSection}
            onClick={() => setCreatingSection(true)}
            data-qa="create-section-button"
          />
        )}

        {creatingSection && (
          <TemplateSectionModal
            onSave={(newSection) => {
              sections.update((old) => [...old, newSection])
              setCreatingSection(false)
            }}
            onCancel={() => setCreatingSection(false)}
          />
        )}
      </ContentArea>

      <Gap />
      <ContentArea opaque>
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <Button
            text={i18n.common.goBack}
            onClick={() => navigate('/document-templates')}
          />

          {featureFlags.forceUnpublishDocumentTemplate &&
            template.published && (
              <ConfirmedMutation
                buttonStyle="BUTTON"
                buttonText={
                  i18n.documentTemplates.templateEditor.forceUnpublish.button
                }
                confirmationTitle={
                  i18n.documentTemplates.templateEditor.forceUnpublish
                    .confirmationTitle
                }
                confirmationText={
                  i18n.documentTemplates.templateEditor.forceUnpublish
                    .confirmationText
                }
                mutation={forceUnpublishDocumentTemplateMutation}
                onClick={() => ({ templateId: template.id })}
              />
            )}

          {!readOnly && (
            <FixedSpaceRow alignItems="center">
              <Checkbox
                label={i18n.documentTemplates.templateEditor.readyToPublish}
                checked={readyToPublish}
                onChange={setReadyToPublish}
                data-qa="ready-to-publish-checkbox"
              />
              <AsyncButton
                text={i18n.common.save}
                primary
                data-qa="save-template"
                onClick={() =>
                  updateDocumentTemplateContent({
                    templateId: template.id,
                    body: form.value()
                  }).then((res) =>
                    readyToPublish
                      ? publishDocumentTemplate({ templateId: template.id })
                      : res
                  )
                }
                onSuccess={() => navigate('/document-templates')}
              />
            </FixedSpaceRow>
          )}
        </FixedSpaceRow>
      </ContentArea>
    </div>
  )
})

const BasicsSection = React.memo(function BasicsSection({
  template,
  editingAllowed
}: {
  template: DocumentTemplate
  editingAllowed: boolean
}) {
  const { i18n } = useTranslation()
  const [editing, setEditing] = useState(false)

  return editing ? (
    <BasicsEditor template={template} onClose={() => setEditing(false)} />
  ) : (
    <FixedSpaceColumn>
      <FixedSpaceRow justifyContent="space-between">
        <H1>{template.name}</H1>
        {editingAllowed && (
          <InlineButton
            onClick={() => setEditing(true)}
            text="Muokkaa lomakkeen perustietoja"
            icon={faPen}
          />
        )}
      </FixedSpaceRow>
      <FixedSpaceRow justifyContent="space-between" alignItems="flex-start">
        <GrovingDiv>
          <LabelValueList
            spacing="small"
            contents={[
              {
                label: i18n.documentTemplates.templateModal.validity,
                value: template.validity.format()
              },
              {
                label: i18n.documentTemplates.templateModal.language,
                value: i18n.documentTemplates.languages[template.language]
              },
              {
                label: i18n.documentTemplates.templateModal.type,
                value: i18n.documentTemplates.documentTypes[template.type]
              },
              {
                label:
                  i18n.documentTemplates.templateModal.processDefinitionNumber,
                value: template.processDefinitionNumber
              },
              {
                label:
                  i18n.documentTemplates.templateModal.archiveDurationMonths,
                value: template.archiveDurationMonths ?? '-'
              }
            ]}
          />
        </GrovingDiv>
        <FixedSpaceColumn spacing="xxs">
          <span>{template.legalBasis}</span>
          {template.confidential && (
            <span>{i18n.documentTemplates.templateEditor.confidential}</span>
          )}
        </FixedSpaceColumn>
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})

const BasicsEditor = React.memo(function BasicsEditor({
  template,
  onClose
}: {
  template: DocumentTemplate
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()

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
    () => ({
      name: template.name,
      type: {
        domValue: template.type,
        options: typeOptions
      },
      language: {
        domValue: template.language,
        options: languageOptions
      },
      confidential: template.confidential,
      legalBasis: template.legalBasis,
      validity: openEndedLocalDateRange.fromRange(template.validity),
      processDefinitionNumber: template.processDefinitionNumber ?? '',
      archiveDurationMonths: template.archiveDurationMonths?.toString() ?? '0'
    }),
    {
      ...i18n.validationErrors
    }
  )

  const {
    name,
    type,
    language,
    confidential,
    legalBasis,
    validity,
    processDefinitionNumber,
    archiveDurationMonths
  } = useFormFields(form)

  return (
    <FixedSpaceColumn>
      <div>
        <Label>{i18n.documentTemplates.templateModal.name}</Label>
        <InputFieldF bind={name} hideErrorsBeforeTouched data-qa="name-input" />
        <Gap />
        <Label>{i18n.documentTemplates.templateModal.validity}</Label>
        <DateRangePickerF bind={validity} locale={lang} />
        <Gap />
        <Label>{i18n.documentTemplates.templateModal.type}</Label>
        <SelectF bind={type} data-qa="type-select" />
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
          info={
            i18n.documentTemplates.templateModal.processDefinitionNumberInfo
          }
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
      </div>
      <FixedSpaceRow justifyContent="flex-end">
        <Button onClick={onClose} text={i18n.common.cancel} />
        <MutateButton
          primary
          mutation={updateDocumentTemplateBasicsMutation}
          disabled={!form.isValid()}
          onClick={() => ({ templateId: template.id, body: form.value() })}
          text={i18n.common.save}
          onSuccess={onClose}
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})

const GrovingDiv = styled.div`
  flex-grow: 1;
`
