// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { oneOf, required } from 'lib-common/form/form'
import { useForm } from 'lib-common/form/hooks'
import {
  ChildDocumentSummaryWithPermittedActions,
  DocumentTemplateSummary
} from 'lib-common/generated/api-types/document'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { activeDocumentTemplateSummariesQuery } from '../document-templates/queries'

import { childDocumentsQuery, createChildDocumentMutation } from './queries'

const WiderTd = styled(Td)`
  width: 50%;
`

const ChildDocuments = React.memo(function ChildDocuments({
  childId,
  documents
}: {
  childId: UUID
  documents: ChildDocumentSummaryWithPermittedActions[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <div>
      <Table data-qa="table-of-child-documents">
        <Thead>
          <Tr>
            <Th>{i18n.childInformation.childDocuments.table.document}</Th>
            <Th>{i18n.childInformation.childDocuments.table.modified}</Th>
            <Th>{i18n.childInformation.childDocuments.table.published}</Th>
            <Th>{i18n.childInformation.childDocuments.table.status}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {orderBy(
            documents,
            (document) => document.data.modifiedAt,
            'desc'
          ).map(({ data: document, permittedActions }) => (
            <Tr key={document.id} data-qa="child-document-row">
              <WiderTd data-qa={`child-document-${document.id}`}>
                <Button
                  appearance="inline"
                  aria-label={i18n.childInformation.childDocuments.table.open}
                  text={document.templateName}
                  icon={faFile}
                  disabled={!permittedActions.includes('READ')}
                  onClick={() =>
                    navigate({
                      pathname: `/child-documents/${document.id}`,
                      search: `?childId=${childId}`
                    })
                  }
                  data-qa="open-document"
                />
              </WiderTd>
              <Td>{document.modifiedAt.format()}</Td>
              <Td data-qa="document-published-at">
                {document.publishedAt?.format() ?? '-'}
              </Td>
              <Td data-qa="document-status">
                <ChildDocumentStateChip status={document.status} />
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </div>
  )
})

const CreationModal = React.memo(function CreationModal({
  childId,
  templates,
  onClose
}: {
  childId: UUID
  templates: DocumentTemplateSummary[]
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: createChildDocument } = useMutationResult(
    createChildDocumentMutation
  )
  const navigate = useNavigate()

  const form = required(oneOf<UUID>())
  const bind = useForm(
    form,
    () => ({
      domValue: templates[0]?.id,
      options: templates.map((t) => ({
        domValue: t.id,
        value: t.id,
        label: t.name
      }))
    }),
    i18n.validationErrors
  )

  const submit = async () => {
    const res = await createChildDocument({
      body: {
        childId,
        templateId: bind.value()
      }
    })
    if (res.isSuccess) {
      navigate(`/child-documents/${res.value}`)
    }
    return res
  }

  return (
    <AsyncFormModal
      title={i18n.childInformation.childDocuments.addNew}
      resolveAction={submit}
      onSuccess={onClose}
      resolveLabel={i18n.common.confirm}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!bind.isValid()}
    >
      <FixedSpaceColumn>
        <Label>{i18n.childInformation.childDocuments.select}</Label>
        <SelectF bind={bind} data-qa="template-select" />
      </FixedSpaceColumn>
    </AsyncFormModal>
  )
})

export default React.memo(function ChildDocumentsList({
  childId
}: {
  childId: UUID
}) {
  const { i18n } = useTranslation()
  const documentsResult = useQueryResult(childDocumentsQuery({ childId }))
  const documentTemplatesResult = useQueryResult(
    activeDocumentTemplateSummariesQuery({ childId })
  )
  const [creationModalOpen, setCreationModalOpen] = useState(false)

  return renderResult(
    combine(documentsResult, documentTemplatesResult),
    ([documents, templates]) => {
      const validTemplates = templates.filter(
        (template) =>
          !documents.some(
            ({ data: doc }) =>
              doc.templateId === template.id && doc.status !== 'COMPLETED'
          ) && !template.type.startsWith('MIGRATED_')
      )

      return (
        <FixedSpaceColumn>
          <AddButtonRow
            text={i18n.childInformation.childDocuments.addNew}
            onClick={() => setCreationModalOpen(true)}
            disabled={validTemplates.length < 1}
            data-qa="create-document"
          />

          {creationModalOpen && (
            <CreationModal
              childId={childId}
              templates={validTemplates}
              onClose={() => setCreationModalOpen(false)}
            />
          )}

          <ChildDocuments childId={childId} documents={documents} />
        </FixedSpaceColumn>
      )
    }
  )
})
