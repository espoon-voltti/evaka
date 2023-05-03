import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { swapElements } from 'lib-common/array'
import { useForm, useFormElems, useFormField } from 'lib-common/form/hooks'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import {
  deserializeDocumentTemplateContentToTemplateContentForm,
  serializeTemplateContentFormToDocumentTemplateContent,
  templateContentForm
} from '../forms'
import {
  publishDocumentTemplateMutation,
  updateDocumentTemplateContentMutation
} from '../queries'

import TemplateSectionModal from './TemplateSectionModal'
import TemplateSectionView from './TemplateSectionView'

interface Props {
  templateId: UUID
  templateContent: DocumentTemplateContent
  readOnly: boolean
}

export default React.memo(function TemplateContentEditor({
  templateId,
  templateContent,
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
    () =>
      deserializeDocumentTemplateContentToTemplateContentForm(templateContent),
    {
      ...i18n.validationErrors
    }
  )
  const sections = useFormField(form, 'sections')
  const sectionElems = useFormElems(sections)

  return (
    <div>
      <ContentArea opaque>
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
            text="Uusi osio"
            onClick={() => setCreatingSection(true)}
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
                label="Valmis julkaistavaksi"
                checked={readyToPublish}
                onChange={setReadyToPublish}
              />
              <AsyncButton
                text={i18n.common.save}
                primary
                data-qa="save-template"
                onClick={() =>
                  updateDocumentTemplateContent({
                    id: templateId,
                    content:
                      serializeTemplateContentFormToDocumentTemplateContent(
                        form.state
                      )
                  }).then((res) =>
                    readyToPublish ? publishDocumentTemplate(templateId) : res
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
