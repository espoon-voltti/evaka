// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import { formatInTimeZone } from 'date-fns-tz'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useForm } from 'lib-common/form/hooks'
import {
  ChildDocumentWithPermittedActions,
  DocumentContent
} from 'lib-common/generated/api-types/document'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Button from 'lib-components/atoms/buttons/Button'
import Spinner from 'lib-components/atoms/state/Spinner'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import DocumentView from 'lib-components/document-templates/DocumentView'
import {
  documentForm,
  getDocumentFormInitialState
} from 'lib-components/document-templates/documents'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import {
  childDocumentNextStatusMutation,
  childDocumentPrevStatusMutation,
  childDocumentQuery,
  deleteChildDocumentMutation,
  publishChildDocumentMutation,
  queryKeys,
  updateChildDocumentContentMutation
} from '../child-information/queries'

import { getNextDocumentStatus, getPrevDocumentStatus } from './statuses'

const ActionBar = styled.div`
  position: sticky;
  z-index: 1;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  margin-top: ${defaultMargins.L};
  padding: ${defaultMargins.s} 0;
`

const ChildDocumentEditorView = React.memo(function ChildDocumentEditorView({
  documentAndPermissions
}: {
  documentAndPermissions: ChildDocumentWithPermittedActions
}) {
  const { data: document, permittedActions } = documentAndPermissions
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const bind = useForm(
    documentForm,
    () =>
      getDocumentFormInitialState(document.template.content, document.content),
    i18n.validationErrors
  )
  const [preview, setPreview] = useState(document.publishedAt !== null)
  const [lastSaved, setLastSaved] = useState(HelsinkiDateTime.now())
  const [lastSavedContent, setLastSavedContent] = useState(document.content)
  const { mutateAsync: updateChildDocumentContent } = useMutationResult(
    updateChildDocumentContentMutation
  )

  // invalidate cached document on onmount
  const queryClient = useQueryClient()
  useEffect(
    () => () => {
      void queryClient.invalidateQueries(queryKeys.childDocument(document.id), {
        type: 'all'
      })
    },
    [queryClient, document.id]
  )

  const save = useCallback(
    async (content: DocumentContent) => {
      const result = await updateChildDocumentContent({
        id: document.id,
        content
      })
      if (result.isSuccess) {
        setLastSaved(HelsinkiDateTime.now())
        setLastSavedContent(content)
      }
    },
    [updateChildDocumentContent, document.id]
  )

  const saved = useMemo(
    () => bind.isValid() && lastSavedContent === bind.value(),
    [bind, lastSavedContent]
  )

  const debouncedValidContent = useDebounce(
    bind.isValid() ? bind.value() : null,
    1000
  )

  useEffect(() => {
    if (!preview && debouncedValidContent !== null) {
      void save(debouncedValidContent)
    }
  }, [preview, debouncedValidContent, save])

  const goBack = () => navigate(`/child-information/${document.child.id}`)

  const nextStatus = useMemo(
    () => getNextDocumentStatus(document.template.type, document.status),
    [document.template.type, document.status]
  )
  const prevStatus = useMemo(
    () => getPrevDocumentStatus(document.template.type, document.status),
    [document.template.type, document.status]
  )

  return (
    <div>
      <Container>
        <ContentArea opaque>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <FixedSpaceColumn>
              <H1 noMargin>{document.template.name}</H1>
              <H2 noMargin>
                {document.child.firstName} {document.child.lastName} (
                {document.child.dateOfBirth?.format()})
              </H2>
            </FixedSpaceColumn>
            <FixedSpaceColumn
              spacing="xxs"
              justifyContent="start"
              alignItems="flex-end"
            >
              <ChildDocumentStateChip status={document.status} />
              {document.template.confidential && (
                <strong>
                  {i18n.documentTemplates.templateEditor.confidential}
                </strong>
              )}
              {!!document.template.legalBasis && (
                <span>{document.template.legalBasis}</span>
              )}
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <Gap size="XXL" />
          <DocumentView bind={bind} readOnly={preview} />
        </ContentArea>
      </Container>

      <ActionBar>
        <Container>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <FixedSpaceRow alignItems="center">
              {preview || !permittedActions.includes('UPDATE') ? (
                <Button
                  text={i18n.common.goBack}
                  onClick={goBack}
                  data-qa="return-button"
                />
              ) : (
                <Button
                  text={i18n.common.goBack}
                  onClick={() => save(bind.value()).then(goBack)}
                  data-qa="return-button"
                />
              )}

              {preview &&
                permittedActions.includes('DELETE') &&
                document.status === 'DRAFT' && (
                  <ConfirmedMutation
                    buttonText={
                      i18n.childInformation.childDocuments.editor.deleteDraft
                    }
                    mutation={deleteChildDocumentMutation}
                    onClick={() => ({
                      documentId: document.id,
                      childId: document.child.id
                    })}
                    onSuccess={goBack}
                    confirmationTitle={
                      i18n.childInformation.childDocuments.editor
                        .deleteDraftConfirmTitle
                    }
                  />
                )}
              {preview &&
                permittedActions.includes('PREV_STATUS') &&
                prevStatus != null && (
                  <ConfirmedMutation
                    buttonText={
                      i18n.childInformation.childDocuments.editor
                        .goToPrevStatus[prevStatus]
                    }
                    mutation={childDocumentPrevStatusMutation}
                    onClick={() => ({
                      documentId: document.id,
                      childId: document.child.id,
                      newStatus: prevStatus
                    })}
                    confirmationTitle={
                      i18n.childInformation.childDocuments.editor
                        .goToPrevStatusConfirmTitle[prevStatus]
                    }
                  />
                )}
              {!preview && (
                <FixedSpaceRow alignItems="center" spacing="xs">
                  <span>
                    {i18n.common.saved}{' '}
                    {formatInTimeZone(
                      lastSaved.timestamp,
                      'Europe/Helsinki',
                      'HH:mm:ss'
                    )}
                  </span>
                  {!saved && (
                    <Spinner size={defaultMargins.m} data-qa="saving-spinner" />
                  )}
                </FixedSpaceRow>
              )}
            </FixedSpaceRow>

            <FixedSpaceRow>
              {preview &&
                permittedActions.includes('UPDATE') &&
                document.status !== 'COMPLETED' && (
                  <Button
                    text={i18n.common.edit}
                    onClick={() => setPreview(false)}
                  />
                )}
              {!preview && (
                <Button
                  text={i18n.childInformation.childDocuments.editor.preview}
                  primary
                  onClick={() => setPreview(true)}
                  disabled={!saved}
                  data-qa="preview-button"
                />
              )}
              {preview &&
                permittedActions.includes('PUBLISH') &&
                document.status !== 'COMPLETED' && (
                  <ConfirmedMutation
                    buttonText={
                      i18n.childInformation.childDocuments.editor.publish
                    }
                    mutation={publishChildDocumentMutation}
                    onClick={() => ({
                      documentId: document.id,
                      childId: document.child.id
                    })}
                    confirmationTitle={
                      i18n.childInformation.childDocuments.editor
                        .publishConfirmTitle
                    }
                    confirmationText={
                      i18n.childInformation.childDocuments.editor
                        .publishConfirmText
                    }
                  />
                )}
              {preview &&
                permittedActions.includes('NEXT_STATUS') &&
                nextStatus != null && (
                  <ConfirmedMutation
                    buttonText={
                      i18n.childInformation.childDocuments.editor
                        .goToNextStatus[nextStatus]
                    }
                    primary
                    mutation={childDocumentNextStatusMutation}
                    onClick={() => ({
                      documentId: document.id,
                      childId: document.child.id,
                      newStatus: nextStatus
                    })}
                    confirmationTitle={
                      i18n.childInformation.childDocuments.editor
                        .goToNextStatusConfirmTitle[nextStatus]
                    }
                    confirmationText={
                      i18n.childInformation.childDocuments.editor
                        .publishConfirmText
                    }
                  />
                )}
            </FixedSpaceRow>
          </FixedSpaceRow>
        </Container>
      </ActionBar>
    </div>
  )
})

export default React.memo(function ChildDocumentEditor() {
  const { documentId } = useNonNullableParams()
  const documentResult = useQueryResult(childDocumentQuery(documentId))

  return renderResult(documentResult, (documentAndPermissions) => (
    <ChildDocumentEditorView documentAndPermissions={documentAndPermissions} />
  ))
})
