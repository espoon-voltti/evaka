// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import isEqual from 'lodash/isEqual'
import React, { useMemo, useState } from 'react'
import { useSearchParams, useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import {
  openEndedLocalDateRange,
  string,
  boolean
} from 'lib-common/form/fields'
import { object, required, mapped, array, value } from 'lib-common/form/form'
import type { BoundForm } from 'lib-common/form/hooks'
import { useForm, useFormFields, useFormElems } from 'lib-common/form/hooks'
import type {
  AcceptedChildDecisions,
  ChildDocumentDetails,
  ChildDocumentWithPermittedActions
} from 'lib-common/generated/api-types/document'
import type { Employee } from 'lib-common/generated/api-types/pis'
import type {
  ChildDocumentDecisionId,
  ChildDocumentId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import {
  constantQuery,
  useChainedQuery,
  useQueryResult
} from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  cancelMutation,
  MutateButton
} from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Radio from 'lib-components/atoms/form/Radio'
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
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H1, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faBoxArchive,
  faExclamation,
  faFilePdf,
  fasCheckCircle,
  fasExclamationTriangle
} from 'lib-icons'

import { downloadChildDocument } from '../../generated/api-clients/document'
import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import MetadataSection from '../archive-metadata/MetadataSection'
import { renderResult } from '../async-rendering'
import {
  acceptChildDocumentDecisionMutation,
  acceptedChildDocumentDecisionsQuery,
  annulChildDocumentDecisionMutation,
  childDocumentDecisionMakersQuery,
  childDocumentMetadataQuery,
  childDocumentNextStatusMutation,
  childDocumentPrevStatusMutation,
  childDocumentQuery,
  deleteChildDocumentMutation,
  planArchiveChildDocumentMutation,
  proposeChildDocumentDecisionMutation,
  publishChildDocumentMutation,
  rejectChildDocumentDecisionMutation
} from '../child-information/queries'
import { FlexRow } from '../common/styled/containers'

import { ActionBar, DocumentBasics } from './ChildDocumentEditView'
import {
  getNextDocumentStatus,
  getPrevDocumentStatus,
  isChildDocumentAnnullable,
  isChildDocumentDecidable,
  isChildDocumentEditable,
  isChildDocumentPublishable
} from './statuses'

const formatEmployeeName = (employee: Employee) =>
  `${formatPersonName(employee, 'Last Preferred')} (${employee.email ?? ''})`

const filterEmployees = (inputValue: string, items: readonly Employee[]) =>
  items.filter(
    (emp) =>
      (emp.preferredFirstName ?? emp.firstName)
        .toLowerCase()
        .startsWith(inputValue.toLowerCase()) ||
      emp.lastName.toLowerCase().startsWith(inputValue.toLowerCase())
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

const optionForm = object({
  label: string(),
  value: boolean()
})

type Answer = boolean | undefined

const otherDecisionForm = object({
  id: value<ChildDocumentDecisionId>(),
  templateId: string(),
  label: string(),
  options: array(optionForm),
  endDecision: required(value<Answer>()),
  validity: openEndedLocalDateRange()
})

const endingDecisionIds = mapped(
  object({
    otherDecisions: array(otherDecisionForm)
  }),
  (output): ChildDocumentDecisionId[] =>
    output.otherDecisions
      .filter((decision) => decision.endDecision)
      .map<ChildDocumentDecisionId>((decision) => decision.id)
)

const ChildDocumentReadViewInner = React.memo(
  function ChildDocumentReadViewInner({
    documentAndPermissions,
    childIdFromUrl,
    decisionMakerOptions,
    acceptedDecisions
  }: {
    documentAndPermissions: ChildDocumentWithPermittedActions
    childIdFromUrl: UUID | null
    decisionMakerOptions: Employee[]
    acceptedDecisions: AcceptedChildDecisions[]
  }) {
    const { data: document, permittedActions } = documentAndPermissions
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

    const goToChildProfile = () =>
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
    const [validity, setValidity] = useState<DateRange | undefined>(undefined)

    const endingDecisionsForm = useForm(
      endingDecisionIds,
      () => ({
        otherDecisions: [
          {
            id: '' as ChildDocumentDecisionId,
            templateId: '',
            label: '',
            options: [
              {
                label: '',
                value: true
              },
              {
                label: '',
                value: false
              }
            ],
            endDecision: undefined,
            validity: openEndedLocalDateRange.fromRange(undefined)
          }
        ]
      }),
      i18n.validationErrors
    )

    const updateEndingDecisionsForm = (newValidity: DateRange | undefined) => {
      endingDecisionsForm.update(() => {
        let filteredDecisions = acceptedDecisions
        if (newValidity) {
          filteredDecisions = acceptedDecisions.filter((decision) =>
            decision.validity.overlapsWith(newValidity)
          )
        }
        return {
          otherDecisions: filteredDecisions.map((decision) => ({
            id: decision.id,
            templateId: decision.templateId,
            label: decision.templateName,
            options: [
              {
                label:
                  i18n.childInformation.childDocuments.decisions
                    .otherValidDecisions.options.end,
                value: true
              },
              {
                label:
                  i18n.childInformation.childDocuments.decisions
                    .otherValidDecisions.options.keep,
                value: false
              }
            ],
            endDecision:
              decision.templateId === document.template.id ? true : undefined,
            validity: openEndedLocalDateRange.fromRange(decision.validity)
          }))
        }
      })
    }

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
                            : !document.template.archiveExternally
                              ? i18n.childInformation.childDocuments.editor
                                  .archiveDisabledNotExternallyArchived
                              : document.status !== 'COMPLETED'
                                ? i18n.childInformation.childDocuments.editor
                                    .archiveDisabledNotCompleted
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
                            !(
                              document.template.archiveExternally &&
                              document.archivedAt == null &&
                              document.status === 'COMPLETED'
                            )
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
            <ContentArea opaque>
              {validity && (
                <>
                  <OtherValidDecisionForm
                    validity={validity}
                    endingDecisionsForm={endingDecisionsForm}
                    document={document}
                  />
                  <Gap size="X3L" />
                </>
              )}
              <FixedSpaceRow
                justifyContent="space-between"
                alignItems="flex-end"
              >
                {acceptingDecision && !validity ? (
                  <Button
                    text={i18n.common.cancel}
                    onClick={() => setAcceptingDecision(false)}
                  />
                ) : validity ? (
                  <Button
                    text={i18n.common.cancel}
                    onClick={() => {
                      setValidity(undefined)
                    }}
                    data-qa="cancel-accept-decision-button"
                  />
                ) : (
                  <FixedSpaceRow alignItems="flex-end">
                    <Button
                      text={i18n.common.leavePage}
                      onClick={goToChildProfile}
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
                          onSuccess={goToChildProfile}
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
                          confirmationText={
                            prevStatus === 'DRAFT'
                              ? i18n.childInformation.childDocuments.editor
                                  .goBackToDraftConfirmText
                              : undefined
                          }
                          data-qa="prev-status-button"
                        />
                      )}
                  </FixedSpaceRow>
                )}

                {acceptingDecision && !validity ? (
                  <AcceptDecisionForm
                    document={document}
                    setValidity={setValidity}
                    acceptedDecisions={acceptedDecisions}
                    updateEndingDecisionsForm={updateEndingDecisionsForm}
                  />
                ) : validity ? (
                  <ConfirmedMutation
                    primary
                    buttonText={i18n.common.confirm}
                    confirmationTitle={
                      i18n.childInformation.childDocuments.decisions
                        .acceptConfirmTitle
                    }
                    mutation={acceptChildDocumentDecisionMutation}
                    disabled={!endingDecisionsForm.isValid()}
                    onClick={() => ({
                      documentId: document.id,
                      childId: document.child.id,
                      body: {
                        validity: validity,
                        endingDecisionIds: endingDecisionsForm.value()
                      }
                    })}
                    data-qa="confirm-other-decisions-button"
                  />
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
                          disabled={
                            isDecision && document.decisionMaker === null
                          }
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
                          extraConfirmationCheckboxText={
                            nextStatus === 'COMPLETED'
                              ? i18n.childInformation.childDocuments.editor
                                  .extraConfirmCompletion
                              : undefined
                          }
                          modalIcon={faExclamation}
                          modalType="warning"
                          data-qa="next-status-button"
                        />
                      )}

                    {permittedActions.includes('REJECT_DECISION') &&
                      isDecidable && (
                        <ConfirmedMutation
                          buttonText={
                            i18n.childInformation.childDocuments.decisions
                              .reject
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
                            i18n.childInformation.childDocuments.decisions
                              .accept
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
                          openAbove
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
            </ContentArea>
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
  document,
  setValidity,
  acceptedDecisions,
  updateEndingDecisionsForm
}: {
  document: ChildDocumentDetails
  setValidity: (validity: DateRange | undefined) => void
  acceptedDecisions: AcceptedChildDecisions[]
  updateEndingDecisionsForm: (validity: DateRange | undefined) => void
}) {
  const { i18n, lang } = useTranslation()

  const [confirming, setConfirming] = React.useState(false)
  const [decisionsWithSameTemplate, setDecisionsWithSameTemplate] =
    React.useState<AcceptedChildDecisions[]>([])

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
      <Button
        primary
        text={i18n.common.confirm}
        disabled={!form.isValid()}
        onClick={() => {
          const filteredDecisions = acceptedDecisions.filter((decision) =>
            decision.validity.overlapsWith(validity.value())
          )
          const decisionsWithSameTemplate = filteredDecisions.filter(
            (decision) => decision.templateId === document.template.id
          )

          setDecisionsWithSameTemplate(decisionsWithSameTemplate)

          if (
            filteredDecisions.length > 0 &&
            decisionsWithSameTemplate.length !== filteredDecisions.length
          ) {
            setValidity(validity.value())
            updateEndingDecisionsForm(validity.value())
          } else {
            setConfirming(true)
          }
        }}
        data-qa="accept-decision-confirm-button"
      />
      {confirming && (
        <MutateFormModal
          title={
            i18n.childInformation.childDocuments.decisions.acceptConfirmTitle
          }
          resolveMutation={acceptChildDocumentDecisionMutation}
          resolveAction={() => ({
            documentId: document.id,
            childId: document.child.id,
            body: {
              validity: validity.value(),
              endingDecisionIds: decisionsWithSameTemplate.map(
                (decision) => decision.id
              )
            }
          })}
          resolveLabel={i18n.common.confirm}
          onSuccess={() => {
            setConfirming(false)
          }}
          rejectAction={() => {
            setConfirming(false)
          }}
          rejectLabel={i18n.common.cancel}
        />
      )}
    </FixedSpaceRow>
  )
})

const OtherValidDecisionForm = React.memo(function OtherValidDecisionForm({
  validity,
  endingDecisionsForm,
  document
}: {
  validity: DateRange
  endingDecisionsForm: BoundForm<typeof endingDecisionIds>
  document: ChildDocumentDetails
}) {
  const { i18n } = useTranslation()
  const { otherDecisions } = useFormFields(endingDecisionsForm)
  const otherDecisionElems = useFormElems(otherDecisions)

  return (
    <>
      <H1>
        {
          i18n.childInformation.childDocuments.decisions.otherValidDecisions
            .title
        }
      </H1>
      {i18n.childInformation.childDocuments.decisions.otherValidDecisions.description(
        validity
      )}
      <Label>
        {
          i18n.childInformation.childDocuments.decisions.otherValidDecisions
            .label
        }
      </Label>
      {otherDecisionElems.map((otherDecision, index) => (
        <div key={index} data-qa="other-decision">
          {index ? <HorizontalLine /> : ''}
          <FixedSpaceRow
            alignItems="flex-end"
            justifyContent="space-between"
            key={0}
          >
            <P>
              {index + 1 + '. '}
              {otherDecision.state.label + ' ('}
              {otherDecision.state.validity.start + ' - '}
              {(otherDecision.state.validity.end ?? '') + ')'}
            </P>
            <FixedSpaceColumn spacing="xs">
              <OtherValidDecisionRadioButtons
                otherValidDecisionForm={otherDecision}
                document={document}
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </div>
      ))}
    </>
  )
})

const OtherValidDecisionRadioButtons = React.memo(
  function OtherValidDecisionRadioButtons({
    otherValidDecisionForm,
    document
  }: {
    otherValidDecisionForm: BoundForm<typeof otherDecisionForm>
    document: ChildDocumentDetails
  }) {
    const { i18n } = useTranslation()

    const { options, endDecision, templateId } = useFormFields(
      otherValidDecisionForm
    )
    const optionElems = useFormElems(options)

    return optionElems.map((optionElem, index) => (
      <Radio
        key={index}
        label={optionElem.state.label}
        checked={
          (optionElem.state.label ===
            i18n.childInformation.childDocuments.decisions.otherValidDecisions
              .options.end &&
            templateId.value() === document.template.id) ||
          endDecision.state === optionElem.state.value
        }
        disabled={templateId.value() === document.template.id}
        onChange={() => endDecision.set(optionElem.state.value)}
        data-qa={`other-decision-option-${index}`}
      />
    ))
  }
)

export default React.memo(function ChildDocumentReadView() {
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

  const acceptedDecisionsResult = useChainedQuery(
    documentResult.map((document) => {
      return getDocumentCategory(document.data.template.type) === 'decision' &&
        document.permittedActions.includes('READ_ACCEPTED_DECISIONS')
        ? acceptedChildDocumentDecisionsQuery({ documentId })
        : constantQuery([])
    })
  )

  return renderResult(
    combine(documentResult, decisionMakersResult, acceptedDecisionsResult),
    (
      [documentAndPermissions, decisionMakers, acceptedDecisions],
      isReloading
    ) => {
      // do not initialize form with possibly stale data
      if (isReloading) return null

      return (
        <ChildDocumentReadViewInner
          documentAndPermissions={documentAndPermissions}
          childIdFromUrl={childIdFromUrl}
          decisionMakerOptions={decisionMakers}
          acceptedDecisions={acceptedDecisions}
        />
      )
    }
  )
})
