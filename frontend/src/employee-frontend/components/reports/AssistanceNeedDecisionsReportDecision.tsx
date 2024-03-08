// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { shade } from 'polished'
import React, { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { I18nContext, Lang, useTranslation } from 'employee-frontend/state/i18n'
import { UserContext } from 'employee-frontend/state/user'
import { wrapResult } from 'lib-common/api'
import {
  AssistanceNeedDecision,
  AssistanceNeedDecisionStatus
} from 'lib-common/generated/api-types/assistanceneed'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import AssistanceNeedDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedDecisionReadOnly'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { ButtonLink } from 'lib-components/atoms/buttons/ButtonLink'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import InputField from 'lib-components/atoms/form/InputField'
import Content from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { ModalType } from 'lib-components/molecules/modals/BaseModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faQuestion, faTimes } from 'lib-icons'

import {
  annulAssistanceNeedDecision,
  decideAssistanceNeedDecision,
  getAssistanceNeedDecision,
  markAssistanceNeedDecisionAsOpened,
  updateAssistanceNeedDecisionDecisionMaker
} from '../../generated/api-clients/assistanceneed'

import { AssistanceNeedDecisionReportContext } from './AssistanceNeedDecisionReportContext'

const getAssistanceNeedDecisionResult = wrapResult(getAssistanceNeedDecision)
const decideAssistanceNeedDecisionResult = wrapResult(
  decideAssistanceNeedDecision
)
const annulAssistanceNeedDecisionResult = wrapResult(
  annulAssistanceNeedDecision
)
const markAssistanceNeedDecisionAsOpenedResult = wrapResult(
  markAssistanceNeedDecisionAsOpened
)
const updateAssistanceNeedDecisionDecisionMakerResult = wrapResult(
  updateAssistanceNeedDecisionDecisionMaker
)

const StickyFooterContainer = styled.div`
  padding: ${defaultMargins.xs};
`

const DangerAsyncButton = styled(AsyncButton)`
  background-color: ${(p) => p.theme.colors.status.danger};
  color: ${(p) => p.theme.colors.grayscale.g0};
  border-color: transparent;

  &:hover,
  &:active {
    background-color: ${(p) => shade(0.1, p.theme.colors.status.danger)};
    color: ${(p) => p.theme.colors.grayscale.g0};
    border-color: transparent;
  }

  &:disabled {
    color: ${(p) => p.theme.colors.grayscale.g0};
    border-color: ${(p) => p.theme.colors.grayscale.g35};
    background: ${(p) => p.theme.colors.grayscale.g35};
  }
`

export default React.memo(function AssistanceNeedDecisionsReportDecision() {
  const { id } = useRouteParams(['id'])
  const navigate = useNavigate()

  const [assistanceNeedDecision, reloadDecision] = useApiState(
    () => getAssistanceNeedDecisionResult({ id }),
    [id]
  )

  const { user } = useContext(UserContext)
  const { refreshAssistanceNeedDecisionCounts } = useContext(
    AssistanceNeedDecisionReportContext
  )

  useEffect(() => {
    if (!user) {
      return
    }

    void assistanceNeedDecision.map(async ({ decision }) => {
      if (decision.decisionMaker?.employeeId === user?.id) {
        await markAssistanceNeedDecisionAsOpenedResult({ id: decision.id })
        refreshAssistanceNeedDecisionCounts()
      }
    })
  }, [assistanceNeedDecision, user, refreshAssistanceNeedDecisionCounts])

  const {
    i18n: {
      childInformation: { assistanceNeedDecision: t },
      ...i18n
    }
  } = useTranslation()

  const [decisionModalStatus, setDecisionModalStatus] = useState<
    DecisionStatus | 'ANNULLED' | 'APPROVE_FAILED'
  >()

  const [mismatchDecisionMakerModalOpen, setMismatchDecisionMakerModalOpen] =
    useState(false)

  return (
    <>
      {decisionModalStatus === 'ANNULLED' ? (
        <AnnulModal
          onClose={(shouldRefresh) => {
            setDecisionModalStatus(undefined)
            if (shouldRefresh) {
              void reloadDecision()
            }
          }}
          decisionId={id}
        />
      ) : decisionModalStatus === 'APPROVE_FAILED' ? (
        <ApproveFailedModal
          onClose={() => {
            setDecisionModalStatus(undefined)
          }}
        />
      ) : decisionModalStatus !== undefined ? (
        <DecisionModal
          onFailed={() => {
            setDecisionModalStatus('APPROVE_FAILED')
          }}
          onClose={(shouldRefresh) => {
            setDecisionModalStatus(undefined)
            if (shouldRefresh) {
              void reloadDecision()
            }
          }}
          decisionId={id}
          decisionStatus={decisionModalStatus}
        />
      ) : null}

      {mismatchDecisionMakerModalOpen && (
        <MismatchDecisionMakerModal
          onClose={() => {
            setMismatchDecisionMakerModalOpen(false)
            void reloadDecision()
          }}
          decisionId={id}
        />
      )}

      {renderResult(
        assistanceNeedDecision,
        ({ decision, permittedActions }) => (
          <>
            <Content>
              <ReturnButton label={i18n.common.goBack} />

              <I18nContext.Provider
                value={{
                  lang: decision.language.toLowerCase() as Lang,
                  setLang: () => undefined
                }}
              >
                <AssistanceNeedDecisionContent
                  decision={decision}
                  warning={
                    decision.decisionMaker?.employeeId !== user?.id &&
                    permittedActions.includes('UPDATE_DECISION_MAKER') && (
                      <AlertBox
                        message={
                          <>
                            {
                              i18n.reports.assistanceNeedDecisions
                                .mismatchDecisionMakerWarning.text
                            }{' '}
                            <ButtonLink
                              onClick={() =>
                                setMismatchDecisionMakerModalOpen(true)
                              }
                              data-qa="mismatch-modal-link"
                            >
                              {
                                i18n.reports.assistanceNeedDecisions
                                  .mismatchDecisionMakerWarning.link
                              }
                            </ButtonLink>
                          </>
                        }
                      />
                    )
                  }
                />
              </I18nContext.Provider>
            </Content>
            <Gap size="m" />
            <StickyFooter>
              <StickyFooterContainer>
                <FixedSpaceRow justifyContent="space-between" flexWrap="wrap">
                  <FixedSpaceRow spacing="s">
                    <Button
                      onClick={() =>
                        navigate(`/reports/assistance-need-decisions`)
                      }
                    >
                      {t.leavePage}
                    </Button>
                  </FixedSpaceRow>
                  <FixedSpaceRow spacing="m">
                    {(decision.status === 'DRAFT' ||
                      decision.status === 'NEEDS_WORK') &&
                    permittedActions.includes('DECIDE') ? (
                      <>
                        <DangerAsyncButton
                          text={
                            i18n.reports.assistanceNeedDecisions.rejectDecision
                          }
                          onClick={() => setDecisionModalStatus('REJECTED')}
                          onSuccess={() => reloadDecision()}
                          data-qa="reject-button"
                        />
                        <AsyncButton
                          text={
                            i18n.reports.assistanceNeedDecisions
                              .returnDecisionForEditing
                          }
                          onClick={() => setDecisionModalStatus('NEEDS_WORK')}
                          onSuccess={() => reloadDecision()}
                          data-qa="return-for-edit"
                        />
                        <AsyncButton
                          primary
                          text={
                            i18n.reports.assistanceNeedDecisions.approveDecision
                          }
                          onClick={() => setDecisionModalStatus('ACCEPTED')}
                          onSuccess={() => reloadDecision()}
                          data-qa="approve-button"
                        />
                      </>
                    ) : decision.status === 'ACCEPTED' ||
                      (decision.status === 'REJECTED' &&
                        permittedActions.includes('ANNUL')) ? (
                      <DangerAsyncButton
                        text={
                          i18n.reports.assistanceNeedDecisions.annulDecision
                        }
                        onClick={() => setDecisionModalStatus('ANNULLED')}
                        onSuccess={() => reloadDecision()}
                        data-qa="annul-button"
                      />
                    ) : null}
                  </FixedSpaceRow>
                </FixedSpaceRow>
              </StickyFooterContainer>
            </StickyFooter>
          </>
        )
      )}
    </>
  )
})

type DecisionStatus = Exclude<
  AssistanceNeedDecisionStatus,
  'DRAFT' | 'ANNULLED'
>

const getModalI18nKey = (
  decisionStatus: DecisionStatus
): 'approveModal' | 'returnForEditModal' | 'rejectModal' => {
  switch (decisionStatus) {
    case 'ACCEPTED':
      return 'approveModal'
    case 'NEEDS_WORK':
      return 'returnForEditModal'
    case 'REJECTED':
      return 'rejectModal'
    default:
      throw Error('Unknown decision status')
  }
}

const DecisionModal = React.memo(function DecisionModal({
  decisionId,
  onClose,
  onFailed,
  decisionStatus
}: {
  decisionId: UUID
  onClose: (shouldRefresh: boolean) => void
  onFailed: () => void
  decisionStatus: DecisionStatus
}) {
  const { i18n } = useTranslation()

  const t =
    i18n.reports.assistanceNeedDecisions[getModalI18nKey(decisionStatus)]

  const modalTypes: Record<DecisionStatus, ModalType> = {
    ACCEPTED: 'success',
    NEEDS_WORK: 'warning',
    REJECTED: 'danger'
  }

  return (
    <InfoModal
      type={modalTypes[decisionStatus]}
      title={t.title}
      text={t.text}
      icon={decisionStatus === 'REJECTED' ? faTimes : faQuestion}
      reject={{
        action: () => onClose(false),
        label: i18n.common.cancel
      }}
      resolve={{
        async action() {
          const result = await decideAssistanceNeedDecisionResult({
            id: decisionId,
            body: {
              status: decisionStatus
            }
          })
          if (result.isFailure && result.statusCode === 409) {
            onFailed()
          } else {
            onClose(true)
          }
        },
        label: t.okBtn
      }}
    />
  )
})

const ApproveFailedModal = React.memo(function ApproveFailedModal({
  onClose
}: {
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const t = i18n.reports.assistanceNeedDecisions.approveFailedModal

  return (
    <InfoModal
      type="danger"
      title={t.title}
      text={t.text}
      icon={faTimes}
      reject={{
        action: onClose,
        label: i18n.common.cancel
      }}
      resolve={{
        action: onClose,
        label: t.okBtn
      }}
    />
  )
})

const AnnulModal = React.memo(function AnnulModal({
  decisionId,
  onClose
}: {
  decisionId: UUID
  onClose: (shouldRefresh: boolean) => void
}) {
  const { i18n } = useTranslation()
  const t = i18n.reports.assistanceNeedDecisions.annulModal

  const [reason, setReason] = useState('')

  return (
    <InfoModal
      type="danger"
      title={t.title}
      text={t.text}
      icon={faTimes}
      reject={{
        action: () => onClose(false),
        label: i18n.common.cancel
      }}
      resolve={{
        async action() {
          await annulAssistanceNeedDecisionResult({
            id: decisionId,
            body: { reason: reason.trim() }
          })
          onClose(true)
        },
        label: t.okBtn,
        disabled: reason.trim() === ''
      }}
    >
      <InputField
        value={reason}
        onChange={setReason}
        placeholder={t.inputPlaceholder}
        data-qa="annul-reason-input"
      />
    </InfoModal>
  )
})

const MismatchDecisionMakerModal = React.memo(
  function MismatchDecisionMakerModal({
    decisionId,
    onClose
  }: {
    decisionId: UUID
    onClose: () => void
  }) {
    const { i18n } = useTranslation()

    const [title, setTitle] = useState('')

    return (
      <InfoModal
        type="info"
        title={
          i18n.reports.assistanceNeedDecisions.mismatchDecisionMakerModal.title
        }
        text={
          i18n.reports.assistanceNeedDecisions.mismatchDecisionMakerModal.text
        }
        icon={faQuestion}
        reject={{
          action: () => onClose(),
          label: i18n.common.cancel
        }}
        resolve={{
          async action() {
            if (title) {
              await updateAssistanceNeedDecisionDecisionMakerResult({
                id: decisionId,
                body: { title }
              })
              onClose()
            }
          },
          label:
            i18n.reports.assistanceNeedDecisions.mismatchDecisionMakerModal
              .okBtn
        }}
      >
        <InputField
          type="text"
          value={title}
          onChange={setTitle}
          required
          info={
            title.length === 0
              ? {
                  status: 'warning',
                  text: i18n.validationErrors.required
                }
              : undefined
          }
          hideErrorsBeforeTouched
          placeholder={
            i18n.reports.assistanceNeedDecisions.mismatchDecisionMakerModal
              .titlePlaceholder
          }
          data-qa="title-input"
        />
      </InfoModal>
    )
  }
)

const AssistanceNeedDecisionContent = React.memo(
  function AssistanceNeedDecisionContent({
    decision,
    warning
  }: {
    decision: AssistanceNeedDecision
    warning: React.ReactNode
  }) {
    const {
      i18n: {
        childInformation: { assistanceNeedDecision: t }
      }
    } = useTranslation()

    return (
      <AssistanceNeedDecisionReadOnly
        decision={decision}
        decisionMakerWarning={warning}
        texts={t}
      />
    )
  }
)
