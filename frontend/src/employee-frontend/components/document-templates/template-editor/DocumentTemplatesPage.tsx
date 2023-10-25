// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faCheck, faCopy, faPen, faTimes, faTrash } from 'Icons'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import DateRange from 'lib-common/date-range'
import { openEndedLocalDateRange } from 'lib-common/form/fields'
import { required } from 'lib-common/form/form'
import { useForm } from 'lib-common/form/hooks'
import { DocumentTemplateSummary } from 'lib-common/generated/api-types/document'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1 } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import {
  deleteDocumentTemplateMutation,
  documentTemplateSummariesQuery,
  updateDocumentTemplateValidityMutation
} from '../queries'

import TemplateModal from './TemplateModal'

const validityForm = required(openEndedLocalDateRange())

const ValidityEditor = React.memo(function ValidityEditor({
  id,
  validity,
  onClose
}: {
  id: UUID
  validity: DateRange
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const { mutateAsync: updateDocumentTemplateValidity, isIdle } =
    useMutationResult(updateDocumentTemplateValidityMutation)
  const form = useForm(
    validityForm,
    () => openEndedLocalDateRange.fromRange(validity),
    i18n.validationErrors
  )

  return (
    <FixedSpaceRow alignItems="center">
      <DateRangePickerF bind={form} locale={lang} />
      <IconButton
        icon={faCheck}
        aria-label={i18n.common.save}
        onClick={async () => {
          const result = await updateDocumentTemplateValidity({
            id: id,
            validity: form.value()
          })
          if (result.isSuccess) {
            onClose()
          }
        }}
        disabled={!form.isValid() || !isIdle}
      />
      <IconButton
        icon={faTimes}
        aria-label={i18n.common.cancel}
        onClick={onClose}
        disabled={!isIdle}
      />
    </FixedSpaceRow>
  )
})

const TemplateRow = React.memo(function TemplateRow({
  template,
  onDuplicate,
  editingValidity,
  onEditValidity,
  onCloseEditValidity
}: {
  template: DocumentTemplateSummary
  onDuplicate: () => void
  editingValidity: boolean
  onEditValidity: () => void
  onCloseEditValidity: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: deleteDocumentTemplate } = useMutationResult(
    deleteDocumentTemplateMutation
  )

  return (
    <Tr key={template.id}>
      <Td>
        <Link to={`/document-templates/${template.id}`}>{template.name}</Link>
      </Td>
      <Td>{i18n.documentTemplates.documentTypes[template.type]}</Td>
      <Td>{i18n.documentTemplates.languages[template.language]}</Td>
      <Td>
        {editingValidity ? (
          <ValidityEditor
            id={template.id}
            validity={template.validity}
            onClose={onCloseEditValidity}
          />
        ) : (
          <FixedSpaceRow alignItems="center">
            <span>{template.validity.format()}</span>
            <IconButton
              icon={faPen}
              aria-label={i18n.common.edit}
              onClick={onEditValidity}
            />
          </FixedSpaceRow>
        )}
      </Td>
      <Td>
        {template.published
          ? i18n.documentTemplates.templatesPage.published
          : i18n.documentTemplates.templatesPage.draft}
      </Td>
      <Td>
        <FixedSpaceRow>
          <IconButton
            icon={faCopy}
            aria-label={i18n.common.copy}
            onClick={onDuplicate}
          />
          <IconButton
            icon={faTrash}
            aria-label={i18n.common.remove}
            disabled={template.published}
            onClick={() => deleteDocumentTemplate(template.id)}
          />
        </FixedSpaceRow>
      </Td>
    </Tr>
  )
})

export default React.memo(function DocumentTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.documentTemplates

  const [templateModalOpen, setTemplateModalOpen] = useState(false)
  const [duplicatingTemplateId, setDuplicatingTemplateId] = useState<
    string | null
  >(null)
  const [editingValidityId, setEditingValidityId] = useState<string | null>(
    null
  )

  const templates = useQueryResult(documentTemplateSummariesQuery())
  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.title}</H1>
        <AddButtonRow
          onClick={() => {
            setDuplicatingTemplateId(null)
            setTemplateModalOpen(true)
          }}
          text={i18n.documentTemplates.templatesPage.add}
          data-qa="create-template-button"
        />
        {templateModalOpen && (
          <TemplateModal
            duplicateFrom={duplicatingTemplateId}
            onClose={() => {
              setTemplateModalOpen(false)
              setDuplicatingTemplateId(null)
            }}
          />
        )}
        {renderResult(templates, (data) => (
          <>
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.documentTemplates.templatesPage.name}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.type}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.language}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.validity}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.status}</Th>
                  <Th />
                </Tr>
              </Thead>
              <Tbody>
                {data.map((template) => (
                  <TemplateRow
                    key={template.id}
                    template={template}
                    onDuplicate={() => {
                      setDuplicatingTemplateId(template.id)
                      setTemplateModalOpen(true)
                    }}
                    editingValidity={editingValidityId === template.id}
                    onEditValidity={() => setEditingValidityId(template.id)}
                    onCloseEditValidity={() => setEditingValidityId(null)}
                  />
                ))}
              </Tbody>
            </Table>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
