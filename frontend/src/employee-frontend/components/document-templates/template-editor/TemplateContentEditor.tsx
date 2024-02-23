// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { swapElements } from 'lib-common/array'
import { useForm, useFormElems, useFormField } from 'lib-common/form/hooks'
import { DocumentTemplate } from 'lib-common/generated/api-types/document'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import LabelValueList from '../../common/LabelValueList'
import {
  publishDocumentTemplateMutation,
  updateDocumentTemplateContentMutation
} from '../queries'
import { getTemplateFormInitialState, templateContentForm } from '../templates'

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
        <LabelValueList
          spacing="small"
          contents={[
            {
              label: i18n.documentTemplates.templateEditor.language,
              value: i18n.documentTemplates.languages[template.language]
            },
            {
              label: i18n.documentTemplates.templateEditor.legalBasis,
              value: template.legalBasis
            },
            {
              label: i18n.documentTemplates.templateEditor.confidential,
              value: template.confidential ? i18n.common.yes : i18n.common.no
            }
          ]}
        />
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
