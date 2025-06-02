// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useQueryClient } from '@tanstack/react-query'
import isEqual from 'lodash/isEqual'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { useSearchParams, useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import { useForm } from 'lib-common/form/hooks'
import type {
  ChildDocumentDetails,
  DocumentContent,
  DocumentWriteLock
} from 'lib-common/generated/api-types/document'
import type { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { Button } from 'lib-components/atoms/buttons/Button'
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
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import { renderResult } from '../async-rendering'
import {
  childDocumentQuery,
  childDocumentWriteLockQuery,
  updateChildDocumentContentMutation
} from '../child-information/queries'

export const ActionBar = styled.div`
  position: sticky;
  z-index: 1;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  margin-top: ${defaultMargins.L};
  padding: ${defaultMargins.s} 0;

  @media print {
    position: relative;
  }
`

export const DocumentBasics = React.memo(function DocumentBasics({
  document
}: {
  document: ChildDocumentDetails
}) {
  const { i18n } = useTranslation()

  return (
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
        {document.decision && (
          <>
            <div>
              {i18n.childInformation.childDocuments.decisions.decisionNumber}{' '}
              {document.decision.decisionNumber}
            </div>
            <Gap size="xs" />
          </>
        )}
        <ChildDocumentStateChip
          status={document.decision?.status ?? document.status}
        />
        {document.template.confidentiality !== null && (
          <strong>{i18n.documentTemplates.templateEditor.confidential}</strong>
        )}
        {!!document.template.legalBasis && (
          <span>{document.template.legalBasis}</span>
        )}
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})

const ConcurrentEditWarning = React.memo(function ConcurrentEditWarning({
  currentLock,
  documentId,
  childIdFromUrl
}: {
  currentLock?: DocumentWriteLock
  documentId: UUID
  childIdFromUrl: UUID | null
}) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()

  const errorText = currentLock
    ? i18n.childInformation.childDocuments.editor.lockedErrorDetailed(
        currentLock.modifiedByName,
        currentLock.opensAt.toLocalTime().format()
      )
    : i18n.childInformation.childDocuments.editor.lockedError

  const onClose = useCallback(
    () =>
      navigate(
        `/child-documents/${documentId}${childIdFromUrl ? `?childId=${childIdFromUrl}` : ''}`
      ),
    [navigate, documentId, childIdFromUrl]
  )

  return (
    <InfoModal
      data-qa="concurrent-edit-error-modal"
      type="warning"
      title={i18n.childInformation.childDocuments.editor.lockedErrorTitle}
      text={errorText}
      close={onClose}
      closeLabel={i18n.common.close}
      resolve={{
        action: onClose,
        label: i18n.common.close
      }}
    />
  )
})

const ChildDocumentEditViewInner = React.memo(
  function ChildDocumentEditViewInner({
    document,
    childIdFromUrl
  }: {
    document: ChildDocumentDetails
    childIdFromUrl: UUID | null
  }) {
    const { i18n } = useTranslation()
    const [, navigate] = useLocation()
    useTitle(document.template.name, { hideDefault: true })

    const bind = useForm(
      documentForm,
      () =>
        getDocumentFormInitialState(
          document.template.content,
          document.content
        ),
      i18n.validationErrors
    )

    const [lastSaved, setLastSaved] = useState(HelsinkiDateTime.now())
    const [lastSavedContent, setLastSavedContent] = useState(document.content)
    const [error, setError] = useState<'lock' | 'other' | null>(null)

    const { mutateAsync: updateChildDocumentContent, isPending: submitting } =
      useMutationResult(updateChildDocumentContentMutation)

    // invalidate cached document on unmount
    const queryClient = useQueryClient()
    useEffect(
      () => () => {
        void queryClient.invalidateQueries({
          queryKey: childDocumentQuery({ documentId: document.id }).queryKey,
          type: 'all'
        })
      },
      [queryClient, document.id]
    )

    const save = useCallback(
      async (content: DocumentContent) => {
        const result = await updateChildDocumentContent({
          documentId: document.id,
          body: content
        })
        if (result.isSuccess) {
          setLastSaved(HelsinkiDateTime.now())
          setLastSavedContent(content)
        } else {
          if (result.isFailure) {
            setError(result.errorCode === 'invalid-lock' ? 'lock' : 'other')
          }
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
        error === null &&
        debouncedValidContent !== null &&
        !isEqual(lastSavedContent, debouncedValidContent) &&
        !submitting
      ) {
        void save(debouncedValidContent)
      }
    }, [error, debouncedValidContent, lastSavedContent, save, submitting])

    useEffect(() => {
      if (error === 'other') {
        const handle = setTimeout(() => setError(null), 5000)
        return () => clearTimeout(handle)
      }
      return undefined
    }, [error])

    const goBack = () =>
      navigate(`/child-information/${childIdFromUrl ?? document.child.id}`)

    if (error === 'lock') {
      return (
        <ConcurrentEditWarning
          documentId={document.id}
          childIdFromUrl={childIdFromUrl}
        />
      )
    }

    return (
      <div>
        <Container>
          <ContentArea opaque>
            <DocumentBasics document={document} />
            <Gap size="XXL" />
            <DocumentView bind={bind} readOnly={false} />
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

                <FixedSpaceRow alignItems="center" spacing="xs">
                  <span>
                    {i18n.common.saved}{' '}
                    {lastSaved.toLocalTime().format('HH:mm:ss')}
                  </span>
                  {!saved && (
                    <Spinner size={defaultMargins.m} data-qa="saving-spinner" />
                  )}
                  {error === 'other' && (
                    <span>
                      <FontAwesomeIcon
                        icon={faExclamationTriangle}
                        color={colors.status.warning}
                      />{' '}
                      {i18n.childInformation.childDocuments.editor.saveError}
                    </span>
                  )}
                </FixedSpaceRow>
              </FixedSpaceRow>

              <Button
                text={i18n.childInformation.childDocuments.editor.preview}
                primary
                onClick={() =>
                  navigate(
                    `/child-documents/${document.id}${childIdFromUrl ? `?childId=${childIdFromUrl}` : ''}`
                  )
                }
                disabled={!saved}
                data-qa="preview-button"
              />
            </FixedSpaceRow>
          </Container>
        </ActionBar>
      </div>
    )
  }
)

export default React.memo(function ChildDocumentEditView() {
  const documentId = useIdRouteParam<ChildDocumentId>('documentId')
  const [searchParams] = useSearchParams()
  const childIdFromUrl = searchParams.get('childId') // duplicate child workaround

  const documentResult = useQueryResult(childDocumentQuery({ documentId }))
  const lockResult = useQueryResult(childDocumentWriteLockQuery({ documentId }))

  return renderResult(
    combine(documentResult, lockResult),
    ([documentAndPermissions, lock], isReloading) => {
      // do not initialize form with possibly stale data
      if (isReloading) return null

      if (!lock.lockTakenSuccessfully) {
        return (
          <ConcurrentEditWarning
            documentId={documentId}
            childIdFromUrl={childIdFromUrl}
            currentLock={lock.currentLock}
          />
        )
      }

      return (
        <ChildDocumentEditViewInner
          document={documentAndPermissions.data}
          childIdFromUrl={childIdFromUrl}
        />
      )
    }
  )
})
