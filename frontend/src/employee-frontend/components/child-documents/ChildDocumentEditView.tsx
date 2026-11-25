// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useQueryClient } from '@tanstack/react-query'
import isEqual from 'lodash/isEqual'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'
import { Redirect, useSearchParams, useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import { useForm } from 'lib-common/form/hooks'
import type {
  ChildDocumentDetails,
  DocumentContent,
  DocumentWriteLock
} from 'lib-common/generated/api-types/document'
import type { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
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
import { PersonName } from 'lib-components/molecules/PersonNames'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import { getDocumentEditStatus } from '../../generated/api-clients/document'
import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import { useWarnOnUnsavedChanges } from '../../utils/useWarnOnUnsavedChanges'
import { renderResult } from '../async-rendering'
import {
  childDocumentQuery,
  childDocumentWriteLockQuery,
  updateChildDocumentContentMutation
} from '../child-information/queries'

import { isTemplateValidityEnded } from './statuses'

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
    <FixedSpaceRow $justifyContent="space-between" $alignItems="center">
      <FixedSpaceColumn>
        <H1 $noMargin>{document.template.name}</H1>
        <H2 $noMargin>
          <PersonName person={document.child} format="First Last" />{' '}
          {document.child.dateOfBirth?.format()}
        </H2>
      </FixedSpaceColumn>
      <FixedSpaceColumn
        $spacing="xxs"
        $justifyContent="start"
        $alignItems="flex-end"
      >
        {document.decision && (
          <>
            <div>
              {i18n.childInformation.childDocuments.decisions.decisionNumber}{' '}
              {document.decision.decisionNumber}
            </div>
            <Gap $size="xs" />
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
        currentLock.lockedByName,
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

const DocumentModifiedInfo = React.memo(function DocumentModifiedInfo({
  documentId,
  childIdFromUrl
}: {
  documentId: UUID
  childIdFromUrl: UUID | null
}) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()

  const onClose = useCallback(
    () =>
      navigate(
        `/child-documents/${documentId}${childIdFromUrl ? `?childId=${childIdFromUrl}` : ''}`
      ),
    [navigate, documentId, childIdFromUrl]
  )

  return (
    <InfoModal
      data-qa="document-modified-modal"
      type="info"
      title={i18n.childInformation.childDocuments.editor.documentModifiedTitle}
      text={i18n.childInformation.childDocuments.editor.documentModifiedText}
      close={onClose}
      closeLabel={i18n.common.close}
      resolve={{
        action: onClose,
        label: i18n.common.ok
      }}
    />
  )
})

const idleMinutesBeforeEditStatusCheck = 1

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
    const [lastSavedContent, setLastSavedContent] = useState(() =>
      bind.isValid() ? bind.value() : document.content
    )
    // A ref, so that an auto-save starting before the next render still sends the latest version
    const contentVersion = useRef(document.contentVersion)
    const saveInProgress = useRef(false)
    // Used for detecting a save that started during an edit status check
    const savesStarted = useRef(0)
    const [saveFailed, setSaveFailed] = useState(false)
    const [lockLost, setLockLost] = useState<{
      currentLock: DocumentWriteLock | undefined
    } | null>(null)
    const [documentModified, setDocumentModified] = useState(false)
    const [templateValidityEnded, setTemplateValidityEnded] = useState(false)
    const editStatusCheckInProgress = useRef(false)
    const editStatusCheckedAt = useRef(HelsinkiDateTime.now())

    const editingInterrupted =
      lockLost !== null || documentModified || templateValidityEnded

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
        saveInProgress.current = true
        savesStarted.current += 1
        const result = await updateChildDocumentContent({
          documentId: document.id,
          body: { content, expectedContentVersion: contentVersion.current }
        })
        saveInProgress.current = false
        if (result.isSuccess) {
          contentVersion.current = result.value.contentVersion
          setLastSaved(HelsinkiDateTime.now())
          setLastSavedContent(content)
          editStatusCheckedAt.current = HelsinkiDateTime.now()
        } else if (result.isFailure) {
          if (result.errorCode === 'template-validity-ended') {
            setTemplateValidityEnded(true)
          } else if (result.errorCode === 'invalid-lock') {
            setLockLost({ currentLock: undefined })
          } else if (result.errorCode === 'document-modified') {
            setDocumentModified(true)
          } else {
            setSaveFailed(true)
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
        !saveFailed &&
        !editingInterrupted &&
        debouncedValidContent !== null &&
        !isEqual(lastSavedContent, debouncedValidContent) &&
        !submitting
      ) {
        void save(debouncedValidContent)
      }
    }, [
      saveFailed,
      editingInterrupted,
      debouncedValidContent,
      lastSavedContent,
      save,
      submitting
    ])

    // Retry a failed auto-save every five seconds
    useEffect(() => {
      if (saveFailed) {
        const handle = setTimeout(() => setSaveFailed(false), 5000)
        return () => clearTimeout(handle)
      }
      return undefined
    }, [saveFailed])

    const checkEditStatus = useCallback(async () => {
      if (
        editingInterrupted ||
        saveInProgress.current ||
        editStatusCheckInProgress.current
      )
        return
      editStatusCheckInProgress.current = true
      const savesStartedBeforeCheck = savesStarted.current
      try {
        const status = await getDocumentEditStatus({ documentId: document.id })
        editStatusCheckedAt.current = HelsinkiDateTime.now()
        if (status.activeLockByOther) {
          setLockLost({ currentLock: status.activeLockByOther })
        } else if (status.contentVersion !== contentVersion.current) {
          // Abort the check if a save started during it, since that save may have changed
          // the version. The server rejects the save if the document has been modified.
          if (savesStarted.current !== savesStartedBeforeCheck) return
          setDocumentModified(true)
        }
      } catch {
        // A failed check is harmless, because the server rejects saves to a modified document
      } finally {
        editStatusCheckInProgress.current = false
      }
    }, [editingInterrupted, document.id])

    // Check the edit status when the user returns to the tab or window
    useEffect(() => {
      const onVisibilityChange = () => {
        if (window.document.visibilityState === 'visible')
          void checkEditStatus()
      }
      const onFocus = () => void checkEditStatus()
      window.document.addEventListener('visibilitychange', onVisibilityChange)
      window.addEventListener('focus', onFocus)
      return () => {
        window.document.removeEventListener(
          'visibilitychange',
          onVisibilityChange
        )
        window.removeEventListener('focus', onFocus)
      }
    }, [checkEditStatus])

    // Check the edit status when the user starts editing the form after being idle
    useEffect(() => {
      if (
        HelsinkiDateTime.now().isEqualOrAfter(
          editStatusCheckedAt.current.addMinutes(
            idleMinutesBeforeEditStatusCheck
          )
        )
      ) {
        void checkEditStatus()
      }
    }, [bind.state, checkEditStatus])

    const hasPendingChanges = !saved && !editingInterrupted
    useWarnOnUnsavedChanges(
      hasPendingChanges,
      i18n.childInformation.childDocuments.editor.unsavedChangesWarning
    )

    // Navigating from an effect lets the unsaved changes guard be released first
    useEffect(() => {
      if (templateValidityEnded) {
        navigate(
          `/child-documents/${document.id}${childIdFromUrl ? `?childId=${childIdFromUrl}` : ''}`
        )
      }
    }, [templateValidityEnded, navigate, document.id, childIdFromUrl])

    const goBack = () =>
      navigate(`/child-information/${childIdFromUrl ?? document.child.id}`)

    if (lockLost) {
      return (
        <ConcurrentEditWarning
          documentId={document.id}
          childIdFromUrl={childIdFromUrl}
          currentLock={lockLost.currentLock}
        />
      )
    }

    if (documentModified) {
      return (
        <DocumentModifiedInfo
          documentId={document.id}
          childIdFromUrl={childIdFromUrl}
        />
      )
    }

    return (
      <div>
        <Container>
          <ContentArea $opaque>
            <DocumentBasics document={document} />
            <Gap $size="XXL" />
            <DocumentView bind={bind} readOnly={false} />
          </ContentArea>
        </Container>

        <ActionBar>
          <Container>
            <FixedSpaceRow $justifyContent="space-between" $alignItems="center">
              <FixedSpaceRow $alignItems="center">
                <Button
                  text={i18n.common.goBack}
                  onClick={goBack}
                  // disable while debounce or save is pending to avoid leaving before saving
                  disabled={
                    submitting ||
                    (bind.isValid() &&
                      !isEqual(bind.value(), debouncedValidContent))
                  }
                  data-qa="return-button"
                />

                <FixedSpaceRow $alignItems="center" $spacing="xs">
                  <span>
                    {i18n.common.saved}{' '}
                    {lastSaved.toLocalTime().format('HH:mm:ss')}
                  </span>
                  {!saved && (
                    <Spinner
                      $size={defaultMargins.m}
                      data-qa="saving-spinner"
                    />
                  )}
                  {saveFailed && (
                    <span data-qa="save-error">
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
  const templateValidityEnded = documentResult
    .map(({ data }) =>
      isTemplateValidityEnded(
        data.template.validity,
        LocalDate.todayInHelsinkiTz()
      )
    )
    .getOrElse(undefined)
  const lockResult = useQueryResult(
    childDocumentWriteLockQuery({ documentId }),
    { enabled: templateValidityEnded === false }
  )

  if (templateValidityEnded) {
    return (
      <Redirect
        to={`/child-documents/${documentId}${childIdFromUrl ? `?childId=${childIdFromUrl}` : ''}`}
        replace
      />
    )
  }

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
