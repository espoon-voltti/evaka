// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { oneOf, required } from 'lib-common/form/form'
import { useForm } from 'lib-common/form/hooks'
import {
  ChildDocumentSummaryWithPermittedActions,
  DocumentTemplateSummary
} from 'lib-common/generated/api-types/document'
import {
  ChildId,
  DocumentTemplateId
} from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import { isInternal } from 'lib-components/document-templates/documents'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H3, Label } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { activeDocumentTemplateSummariesQuery } from '../document-templates/queries'

import { childDocumentsQuery, createChildDocumentMutation } from './queries'

const WiderTd = styled(Td)`
  width: 50%;
`

const InternalChildDocuments = React.memo(function InternalChildDocuments({
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
      <Table data-qa="table-of-internal-child-documents">
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

const ExternalChildDocuments = React.memo(function ExternalChildDocuments({
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
      <Table data-qa="table-of-external-child-documents">
        <Thead>
          <Tr>
            <Th>{i18n.childInformation.childDocuments.table.document}</Th>
            <Th>{i18n.childInformation.childDocuments.table.sent}</Th>
            <Th>{i18n.childInformation.childDocuments.table.answered}</Th>
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
              <Td data-qa="document-sent-at">
                {document.publishedAt !== null ? (
                  <span>{document.publishedAt.toLocalDate().format()}</span>
                ) : (
                  <span>
                    {i18n.childInformation.childDocuments.table.notSent}
                  </span>
                )}
              </Td>
              <Td data-qa="document-answered-at">
                {document.answeredAt !== null ? (
                  <>
                    <span>{document.answeredAt?.toLocalDate().format()}</span>
                    {document.answeredBy !== null && (
                      <span>, {document.answeredBy.name}</span>
                    )}
                  </>
                ) : (
                  <span>
                    {i18n.childInformation.childDocuments.table.unanswered}
                  </span>
                )}
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

const ChildDocumentTables = ({
  childId,
  hasCreatePermission,
  documents,
  templates
}: {
  childId: ChildId
  hasCreatePermission: boolean
  documents: ChildDocumentSummaryWithPermittedActions[]
  templates: DocumentTemplateSummary[]
}) => {
  const { i18n } = useTranslation()
  const [creationModalState, setCreationModalState] = useState<
    'internal' | 'external' | undefined
  >(undefined)
  const [validInternalTemplates, validExternalTemplates] = useMemo(
    () =>
      partition(
        templates.filter(
          (template) =>
            !documents.some(
              ({ data: doc }) =>
                doc.templateId === template.id && doc.status !== 'COMPLETED'
            ) && !template.type.startsWith('MIGRATED_')
        ),
        (template) => isInternal(template.type)
      ),
    [documents, templates]
  )
  const [title, validTemplates] = useMemo(() => {
    switch (creationModalState) {
      case 'internal':
        return [
          i18n.childInformation.childDocuments.addNew.internal,
          validInternalTemplates
        ]
      case 'external':
        return [
          i18n.childInformation.childDocuments.addNew.external,
          validExternalTemplates
        ]
      case undefined:
        return ['', []]
    }
  }, [
    creationModalState,
    i18n.childInformation.childDocuments.addNew.external,
    i18n.childInformation.childDocuments.addNew.internal,
    validExternalTemplates,
    validInternalTemplates
  ])
  const [internalDocuments, externalDocuments] = useMemo(
    () => partition(documents, (document) => isInternal(document.data.type)),
    [documents]
  )

  return (
    <FixedSpaceColumn>
      {creationModalState && (
        <CreationModal
          childId={childId}
          title={title}
          templates={validTemplates}
          onClose={() => setCreationModalState(undefined)}
        />
      )}

      <FixedSpaceRow justifyContent="space-between">
        <H3>{i18n.childInformation.childDocuments.title.internal}</H3>
        {hasCreatePermission && (
          <AddButtonRow
            text={i18n.childInformation.childDocuments.addNew.internal}
            onClick={() => setCreationModalState('internal')}
            disabled={validInternalTemplates.length < 1}
            data-qa="create-internal-document"
          />
        )}
      </FixedSpaceRow>
      <InternalChildDocuments childId={childId} documents={internalDocuments} />

      {featureFlags.citizenChildDocumentTypes && (
        <>
          <FixedSpaceRow justifyContent="space-between">
            <H3>{i18n.childInformation.childDocuments.title.external}</H3>
            {hasCreatePermission && (
              <AddButtonRow
                text={i18n.childInformation.childDocuments.addNew.external}
                onClick={() => setCreationModalState('external')}
                disabled={validExternalTemplates.length < 1}
                data-qa="create-external-document"
              />
            )}
          </FixedSpaceRow>
          <ExternalChildDocuments
            childId={childId}
            documents={externalDocuments}
          />
        </>
      )}
    </FixedSpaceColumn>
  )
}

const CreationModal = React.memo(function CreationModal({
  childId,
  title,
  templates,
  onClose
}: {
  childId: ChildId
  title: string
  templates: DocumentTemplateSummary[]
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: createChildDocument } = useMutationResult(
    createChildDocumentMutation
  )
  const navigate = useNavigate()

  const form = required(oneOf<DocumentTemplateId>())
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
      void navigate(`/child-documents/${res.value}`)
    }
    return res
  }

  return (
    <AsyncFormModal
      title={title}
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
  childId,
  hasCreatePermission
}: {
  childId: ChildId
  hasCreatePermission: boolean
}) {
  const documentsResult = useQueryResult(childDocumentsQuery({ childId }))
  const documentTemplatesResult = useQueryResult(
    hasCreatePermission
      ? activeDocumentTemplateSummariesQuery({ childId })
      : constantQuery([])
  )

  return renderResult(
    combine(documentsResult, documentTemplatesResult),
    ([documents, templates]) => (
      <ChildDocumentTables
        childId={childId}
        hasCreatePermission={hasCreatePermission}
        documents={documents}
        templates={templates}
      />
    )
  )
})
