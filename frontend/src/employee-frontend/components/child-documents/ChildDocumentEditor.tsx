// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useQueryClient } from '@tanstack/react-query'
import { fasCheckCircle, fasExclamationTriangle } from 'Icons'
import { formatInTimeZone } from 'date-fns-tz'
import isEqual from 'lodash/isEqual'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { useForm } from 'lib-common/form/hooks'
import {
  ChildDocumentWithPermittedActions,
  DocumentContent
} from 'lib-common/generated/api-types/document'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
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
import { Gap, defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
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
  documentAndPermissions,
  childIdFromUrl
}: {
  documentAndPermissions: ChildDocumentWithPermittedActions
  childIdFromUrl: UUID | null
}) {
  const { data: document, permittedActions } = documentAndPermissions
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  useEffect(
    () => setTitle(document.template.name, true),
    [document.template.name, setTitle]
  )
  const navigate = useNavigate()
  const bind = useForm(
    documentForm,
    () =>
      getDocumentFormInitialState(document.template.content, document.content),
    i18n.validationErrors
  )
  const [editMode, setEditMode] = useState(false)
  const [lastSaved, setLastSaved] = useState(HelsinkiDateTime.now())
  const [lastSavedContent, setLastSavedContent] = useState(document.content)
  const { mutateAsync: updateChildDocumentContent, isPending: submitting } =
    useMutationResult(updateChildDocumentContentMutation)

  // invalidate cached document on onmount
  const queryClient = useQueryClient()
  useEffect(
    () => () => {
      void queryClient.invalidateQueries({
        queryKey: queryKeys.childDocument(document.id),
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
    () => bind.isValid() && isEqual(lastSavedContent, bind.value()),
    [bind, lastSavedContent]
  )

  const debouncedValidContent = useDebounce(
    bind.isValid() ? bind.value() : null,
    1000
  )

  useEffect(() => {
    if (
      editMode &&
      debouncedValidContent !== null &&
      !isEqual(lastSavedContent, debouncedValidContent) &&
      !submitting
    ) {
      void save(debouncedValidContent)
    }
  }, [editMode, debouncedValidContent, lastSavedContent, save, submitting])

  const goBack = () =>
    navigate(`/child-information/${childIdFromUrl ?? document.child.id}`)

  const nextStatus = useMemo(
    () => getNextDocumentStatus(document.template.type, document.status),
    [document.template.type, document.status]
  )
  const prevStatus = useMemo(
    () => getPrevDocumentStatus(document.template.type, document.status),
    [document.template.type, document.status]
  )

  const publishedUpToDate = useMemo(
    () =>
      document.publishedContent !== null &&
      isEqual(document.publishedContent, lastSavedContent),
    [document.publishedContent, lastSavedContent]
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
          <DocumentView bind={bind} readOnly={!editMode} />
        </ContentArea>
      </Container>

      <ActionBar>
        <Container>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <FixedSpaceRow alignItems="center">
              <Button
                text={i18n.common.goBack}
                onClick={goBack}
                // disable while debounce is pending to avoid leaving before saving
                disabled={
                  bind.isValid() &&
                  !isEqual(bind.value(), debouncedValidContent)
                }
                data-qa="return-button"
              />

              {!editMode &&
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
              {!editMode &&
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
              {editMode && (
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
              {!editMode &&
                permittedActions.includes('UPDATE') &&
                document.status !== 'COMPLETED' && (
                  <Button
                    text={i18n.common.edit}
                    onClick={() => setEditMode(true)}
                    data-qa="edit-button"
                  />
                )}
              {editMode && (
                <Button
                  text={i18n.childInformation.childDocuments.editor.preview}
                  primary
                  onClick={() => setEditMode(false)}
                  disabled={!saved}
                  data-qa="preview-button"
                />
              )}
              {!editMode &&
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
                    data-qa="publish-button"
                  />
                )}
              {!editMode &&
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
                      nextStatus === 'COMPLETED'
                        ? i18n.childInformation.childDocuments.editor
                            .goToCompletedConfirmText
                        : i18n.childInformation.childDocuments.editor
                            .publishConfirmText
                    }
                    data-qa="next-status-button"
                  />
                )}
            </FixedSpaceRow>
          </FixedSpaceRow>
          {!editMode && (
            <>
              <Gap size="s" />
              <FixedSpaceRow alignItems="center" justifyContent="flex-end">
                <FixedSpaceRow alignItems="center" spacing="xs">
                  {publishedUpToDate ? (
                    <>
                      <FontAwesomeIcon
                        icon={fasCheckCircle}
                        color={colors.status.success}
                        size="lg"
                      />
                      <span>
                        {
                          i18n.childInformation.childDocuments.editor
                            .fullyPublished
                        }
                      </span>
                    </>
                  ) : (
                    <>
                      <FontAwesomeIcon
                        icon={fasExclamationTriangle}
                        color={colors.status.warning}
                        size="lg"
                      />
                      <span>
                        {i18n.childInformation.childDocuments.editor.notFullyPublished(
                          document.publishedAt
                        )}
                      </span>
                    </>
                  )}
                </FixedSpaceRow>
              </FixedSpaceRow>
            </>
          )}
        </Container>
      </ActionBar>
    </div>
  )
})

export default React.memo(function ChildDocumentEditor() {
  const { documentId } = useNonNullableParams()
  const [searchParams] = useSearchParams()
  const documentResult = useQueryResult(childDocumentQuery(documentId))

  return renderResult(documentResult, (documentAndPermissions) => (
    <ChildDocumentEditorView
      documentAndPermissions={documentAndPermissions}
      childIdFromUrl={searchParams.get('childId')}
    />
  ))
})
