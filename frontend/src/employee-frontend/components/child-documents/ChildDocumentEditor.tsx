// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useQueryClient } from '@tanstack/react-query'
import isEqual from 'lodash/isEqual'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { openEndedLocalDateRange } from 'lib-common/form/fields'
import { object, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import {
  ChildDocumentDetails,
  ChildDocumentWithPermittedActions,
  DocumentContent,
  DocumentWriteLock
} from 'lib-common/generated/api-types/document'
import { Employee } from 'lib-common/generated/api-types/pis'
import { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import {
  cancelMutation,
  constantQuery,
  useChainedQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Spinner from 'lib-components/atoms/state/Spinner'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import DocumentView from 'lib-components/document-templates/DocumentView'
import {
  documentForm,
  getDocumentCategory,
  getDocumentFormInitialState
} from 'lib-components/document-templates/documents'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faBoxArchive,
  faExclamationTriangle,
  faFilePdf,
  fasCheckCircle,
  fasExclamationTriangle
} from 'lib-icons'

import { downloadChildDocument } from '../../generated/api-clients/document'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import MetadataSection from '../archive-metadata/MetadataSection'
import { renderResult } from '../async-rendering'
import {
  acceptChildDocumentDecisionMutation,
  annulChildDocumentDecisionMutation,
  childDocumentDecisionMakersQuery,
  childDocumentMetadataQuery,
  childDocumentNextStatusMutation,
  childDocumentPrevStatusMutation,
  childDocumentQuery,
  childDocumentWriteLockQuery,
  deleteChildDocumentMutation,
  planArchiveChildDocumentMutation,
  proposeChildDocumentDecisionMutation,
  publishChildDocumentMutation,
  rejectChildDocumentDecisionMutation,
  updateChildDocumentContentMutation
} from '../child-information/queries'
import { FlexRow } from '../common/styled/containers'

import {
  getNextDocumentStatus,
  getPrevDocumentStatus,
  isChildDocumentAnnullable,
  isChildDocumentDecidable,
  isChildDocumentEditable,
  isChildDocumentPublishable
} from './statuses'

const ActionBar = styled.div`
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

const DocumentBasics = React.memo(function DocumentBasics({
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
  const navigate = useNavigate()

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

const formatEmployeeName = (employee: Employee) =>
  `${employee.lastName ?? ''} ${
    employee.preferredFirstName ?? employee.firstName ?? ''
  } (${employee.email ?? ''})`

const filterEmployees = (inputValue: string, items: readonly Employee[]) =>
  items.filter(
    (emp) =>
      (emp.preferredFirstName ?? emp.firstName)
        .toLowerCase()
        .startsWith(inputValue.toLowerCase()) ||
      emp.lastName.toLowerCase().startsWith(inputValue.toLowerCase())
  )

const ChildDocumentEditViewInner = React.memo(
  function ChildDocumentEditViewInner({
    document,
    childIdFromUrl
  }: {
    document: ChildDocumentDetails
    childIdFromUrl: UUID | null
  }) {
    const { i18n } = useTranslation()
    const navigate = useNavigate()
    const { setTitle } = useContext<TitleState>(TitleContext)
    useEffect(
      () => setTitle(document.template.name, true),
      [document.template.name, setTitle]
    )

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

export const ChildDocumentEditView = React.memo(
  function ChildDocumentEditView() {
    const documentId = useIdRouteParam<ChildDocumentId>('documentId')
    const [searchParams] = useSearchParams()
    const childIdFromUrl = searchParams.get('childId') // duplicate child workaround

    const documentResult = useQueryResult(childDocumentQuery({ documentId }))
    const lockResult = useQueryResult(
      childDocumentWriteLockQuery({ documentId })
    )

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
  }
)

const ChildDocumentMetadataSection = React.memo(
  function ChildDocumentMetadataSection({
    documentId
  }: {
    documentId: ChildDocumentId
  }) {
    const result = useQueryResult(
      childDocumentMetadataQuery({ childDocumentId: documentId })
    )
    return <MetadataSection metadataResult={result} />
  }
)

const ChildDocumentReadViewInner = React.memo(
  function ChildDocumentReadViewInner({
    documentAndPermissions,
    childIdFromUrl,
    decisionMakerOptions
  }: {
    documentAndPermissions: ChildDocumentWithPermittedActions
    childIdFromUrl: UUID | null
    decisionMakerOptions: Employee[]
  }) {
    const { data: document, permittedActions } = documentAndPermissions
    const { i18n } = useTranslation()
    const navigate = useNavigate()
    const { setTitle } = useContext<TitleState>(TitleContext)
    useEffect(
      () => setTitle(document.template.name, true),
      [document.template.name, setTitle]
    )

    const bind = useForm(
      documentForm,
      () =>
        getDocumentFormInitialState(
          document.template.content,
          document.content
        ),
      i18n.validationErrors
    )

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
    const isEditable = useMemo(
      () => isChildDocumentEditable(document.status),
      [document.status]
    )
    const isPublishable = useMemo(
      () => isChildDocumentPublishable(document.template.type, document.status),
      [document.template.type, document.status]
    )
    const isDecision = useMemo(
      () => getDocumentCategory(document.template.type) === 'decision',
      [document.template.type]
    )
    const isDecidable = useMemo(
      () => isChildDocumentDecidable(document.template.type, document.status),
      [document.template.type, document.status]
    )
    const isAnnullable = useMemo(
      () =>
        isChildDocumentAnnullable(
          document.template.type,
          document.status,
          document.decision?.status ?? null
        ),
      [document.template.type, document.status, document.decision?.status]
    )

    const publishedUpToDate = useMemo(
      () =>
        document.publishedContent !== null &&
        isEqual(document.publishedContent, document.content),
      [document]
    )

    const [decisionMaker, setDecisionMaker] = useState<Employee | null>(
      document.decisionMaker
        ? (decisionMakerOptions.find((e) => e.id === document.decisionMaker) ??
            null)
        : null
    )

    const [acceptingDecision, setAcceptingDecision] = useState(false)

    return (
      <div>
        {permittedActions.includes('DOWNLOAD') &&
          document.pdfAvailable &&
          publishedUpToDate && (
            <>
              <Container>
                <FlexRow justifyContent="flex-end">
                  <a
                    href={downloadChildDocument({
                      documentId: document.id
                    }).url.toString()}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <FixedSpaceRow spacing="xs" alignItems="center">
                      <FontAwesomeIcon icon={faFilePdf} />
                      <span>
                        {
                          i18n.childInformation.childDocuments.editor
                            .downloadPdf
                        }
                      </span>
                    </FixedSpaceRow>
                  </a>
                  {featureFlags.archiveIntegrationEnabled &&
                    permittedActions.includes('ARCHIVE') && (
                      <Tooltip
                        tooltip={
                          document.archivedAt
                            ? i18n.childInformation.childDocuments.editor.alreadyArchived(
                                document.archivedAt
                              )
                            : undefined
                        }
                        data-qa="archive-tooltip"
                      >
                        <MutateButton
                          icon={faBoxArchive}
                          text={
                            i18n.childInformation.childDocuments.editor.archive
                          }
                          mutation={planArchiveChildDocumentMutation}
                          onClick={() => ({ documentId: document.id })}
                          data-qa="archive-button"
                          disabled={
                            !document.template.archiveExternally ||
                            document.archivedAt !== null
                          }
                        />
                      </Tooltip>
                    )}
                </FlexRow>
              </Container>
              <Gap size="xs" />
            </>
          )}
        <Container>
          <ContentArea opaque>
            <DocumentBasics document={document} />
            <Gap size="XXL" />
            <DocumentView bind={bind} readOnly={true} />
          </ContentArea>
          {permittedActions.includes('READ_METADATA') && (
            <>
              <Gap />
              <Container>
                <ChildDocumentMetadataSection documentId={document.id} />
              </Container>
            </>
          )}
        </Container>

        <ActionBar>
          <Container>
            <FixedSpaceRow justifyContent="space-between" alignItems="flex-end">
              {acceptingDecision ? (
                <Button
                  text={i18n.common.cancel}
                  onClick={() => setAcceptingDecision(false)}
                />
              ) : (
                <FixedSpaceRow alignItems="flex-end">
                  <Button
                    text={i18n.common.goBack}
                    onClick={goBack}
                    data-qa="return-button"
                  />
                  {permittedActions.includes('DELETE') &&
                    document.status === 'DRAFT' && (
                      <ConfirmedMutation
                        buttonText={
                          i18n.childInformation.childDocuments.editor
                            .deleteDraft
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
                  {permittedActions.includes('PREV_STATUS') &&
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
                          body: {
                            newStatus: prevStatus
                          }
                        })}
                        confirmationTitle={
                          i18n.childInformation.childDocuments.editor
                            .goToPrevStatusConfirmTitle[prevStatus]
                        }
                      />
                    )}
                </FixedSpaceRow>
              )}

              {acceptingDecision ? (
                <AcceptDecisionForm document={document} />
              ) : (
                <FixedSpaceRow alignItems="flex-end">
                  {permittedActions.includes('UPDATE') && isEditable && (
                    <Button
                      text={i18n.common.edit}
                      onClick={() =>
                        navigate(
                          `/child-documents/${document.id}/edit${childIdFromUrl ? `?childId=${childIdFromUrl}` : ''}`
                        )
                      }
                      data-qa="edit-button"
                    />
                  )}

                  {permittedActions.includes('PUBLISH') && isPublishable && (
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

                  {permittedActions.includes('NEXT_STATUS') &&
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
                          body: {
                            newStatus: nextStatus
                          }
                        })}
                        disabled={isDecision && document.decisionMaker === null}
                        confirmationTitle={
                          i18n.childInformation.childDocuments.editor
                            .goToNextStatusConfirmTitle[nextStatus]
                        }
                        confirmationText={
                          isDecision
                            ? undefined
                            : nextStatus === 'COMPLETED'
                              ? i18n.childInformation.childDocuments.editor
                                  .goToCompletedConfirmText
                              : i18n.childInformation.childDocuments.editor
                                  .publishConfirmText
                        }
                        data-qa="next-status-button"
                      />
                    )}

                  {permittedActions.includes('REJECT_DECISION') &&
                    isDecidable && (
                      <ConfirmedMutation
                        buttonText={
                          i18n.childInformation.childDocuments.decisions.reject
                        }
                        mutation={rejectChildDocumentDecisionMutation}
                        onClick={() => ({
                          documentId: document.id,
                          childId: document.child.id
                        })}
                        confirmationTitle={
                          i18n.childInformation.childDocuments.decisions
                            .rejectConfirmTitle
                        }
                        data-qa="reject-decision-button"
                      />
                    )}

                  {permittedActions.includes('ACCEPT_DECISION') &&
                    isDecidable && (
                      <Button
                        text={
                          i18n.childInformation.childDocuments.decisions.accept
                        }
                        primary
                        onClick={() => setAcceptingDecision(true)}
                        data-qa="accept-decision-button"
                      />
                    )}

                  {permittedActions.includes('ANNUL_DECISION') &&
                    isAnnullable && (
                      <ConfirmedMutation
                        buttonText={
                          i18n.childInformation.childDocuments.decisions.annul
                        }
                        mutation={annulChildDocumentDecisionMutation}
                        onClick={() => ({
                          documentId: document.id,
                          childId: document.child.id
                        })}
                        confirmationTitle={
                          i18n.childInformation.childDocuments.decisions
                            .annulConfirmTitle
                        }
                        data-qa="annul-decision-button"
                      />
                    )}
                </FixedSpaceRow>
              )}

              {isDecision &&
                document.status === 'DRAFT' &&
                permittedActions.includes('PROPOSE_DECISION') && (
                  <FixedSpaceRow alignItems="flex-end">
                    <FixedSpaceColumn spacing="xxs">
                      <Label>
                        {
                          i18n.childInformation.childDocuments.editor
                            .decisionMaker
                        }{' '}
                        *
                      </Label>
                      <Combobox
                        items={decisionMakerOptions}
                        selectedItem={decisionMaker}
                        onChange={setDecisionMaker}
                        getItemLabel={formatEmployeeName}
                        filterItems={filterEmployees}
                        getItemDataQa={(e) => e.id}
                        placeholder={i18n.common.select}
                        data-qa="decision-maker-combobox"
                      />
                    </FixedSpaceColumn>
                    <ConfirmedMutation
                      buttonText={
                        i18n.childInformation.childDocuments.editor
                          .goToNextStatus.DECISION_PROPOSAL
                      }
                      primary
                      mutation={proposeChildDocumentDecisionMutation}
                      onClick={() =>
                        decisionMaker
                          ? {
                              documentId: document.id,
                              childId: document.child.id,
                              body: {
                                decisionMaker: decisionMaker.id
                              }
                            }
                          : cancelMutation
                      }
                      disabled={!decisionMaker}
                      confirmationTitle={
                        i18n.childInformation.childDocuments.editor
                          .goToNextStatusConfirmTitle.DECISION_PROPOSAL
                      }
                      data-qa="propose-decision-button"
                    />
                  </FixedSpaceRow>
                )}
            </FixedSpaceRow>

            {isPublishable && (
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
  }
)

const acceptForm = object({
  validity: required(openEndedLocalDateRange())
})

const AcceptDecisionForm = React.memo(function AcceptDecisionForm({
  document
}: {
  document: ChildDocumentDetails
}) {
  const { i18n, lang } = useTranslation()

  const form = useForm(
    acceptForm,
    () => ({
      validity: openEndedLocalDateRange.fromRange(
        new DateRange(LocalDate.todayInHelsinkiTz(), null)
      )
    }),
    i18n.validationErrors
  )

  const { validity } = useFormFields(form)

  return (
    <FixedSpaceRow alignItems="flex-end">
      <FixedSpaceColumn spacing="xs">
        <Label>
          {i18n.childInformation.childDocuments.decisions.validityPeriod}
        </Label>
        <DateRangePickerF
          bind={validity}
          locale={lang}
          data-qa="decision-validity-picker"
        />
      </FixedSpaceColumn>
      <ConfirmedMutation
        primary
        buttonText={i18n.common.confirm}
        confirmationTitle={
          i18n.childInformation.childDocuments.decisions.acceptConfirmTitle
        }
        mutation={acceptChildDocumentDecisionMutation}
        disabled={!form.isValid()}
        onClick={() => ({
          documentId: document.id,
          childId: document.child.id,
          body: {
            validity: validity.value()
          }
        })}
        data-qa="accept-decision-confirm-button"
      />
    </FixedSpaceRow>
  )
})

export const ChildDocumentReadView = React.memo(
  function ChildDocumentReadView() {
    const documentId = useIdRouteParam<ChildDocumentId>('documentId')
    const [searchParams] = useSearchParams()
    const childIdFromUrl = searchParams.get('childId') // duplicate child workaround

    const documentResult = useQueryResult(childDocumentQuery({ documentId }))

    const decisionMakersResult = useChainedQuery(
      documentResult.map((document) =>
        getDocumentCategory(document.data.template.type) === 'decision'
          ? childDocumentDecisionMakersQuery({ documentId })
          : constantQuery([])
      )
    )

    return renderResult(
      combine(documentResult, decisionMakersResult),
      ([documentAndPermissions, decisionMakers], isReloading) => {
        // do not initialize form with possibly stale data
        if (isReloading) return null

        return (
          <ChildDocumentReadViewInner
            documentAndPermissions={documentAndPermissions}
            childIdFromUrl={childIdFromUrl}
            decisionMakerOptions={decisionMakers}
          />
        )
      }
    )
  }
)
