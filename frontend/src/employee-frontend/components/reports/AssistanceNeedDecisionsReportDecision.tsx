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
import { AssistanceNeedDecisionStatus } from 'lib-common/generated/api-types/assistanceneed'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
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
  decideAssistanceNeedDecision,
  getAssistanceNeedDecision,
  markAssistanceNeedDecisionAsOpened,
  updateAssistanceNeedDecisionDecisionMaker
} from '../child-information/assistance-need/decision/api'

import { AssistanceNeedDecisionReportContext } from './AssistanceNeedDecisionReportContext'

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
  const { id } = useNonNullableParams<{ id: UUID }>()
  const navigate = useNavigate()

  const [assistanceNeedDecision, reloadDecision] = useApiState(
    () => getAssistanceNeedDecision(id),
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
        await markAssistanceNeedDecisionAsOpened(decision.id)
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

  const [decisionModalStatus, setDecisionModalStatus] =
    useState<DecisionStatus>()

  const canBeDecided = assistanceNeedDecision
    .map(
      ({ permittedActions, decision }) =>
        permittedActions.includes('DECIDE') &&
        decision.status !== 'ACCEPTED' &&
        decision.status !== 'REJECTED'
    )
    .getOrElse(false)

  const [mismatchDecisionMakerModalOpen, setMismatchDecisionMakerModalOpen] =
    useState(false)

  return (
    <>
      {decisionModalStatus && (
        <DecisionModal
          onClose={(shouldRefresh) => {
            setDecisionModalStatus(undefined)
            if (shouldRefresh) {
              reloadDecision()
            }
          }}
          decisionId={id}
          decisionStatus={decisionModalStatus}
        />
      )}

      {mismatchDecisionMakerModalOpen && (
        <MismatchDecisionMakerModal
          onClose={() => {
            setMismatchDecisionMakerModalOpen(false)
            reloadDecision()
          }}
          decisionId={id}
        />
      )}

      <Content>
        <ReturnButton label={i18n.common.goBack} />

        {renderResult(
          assistanceNeedDecision,
          ({ decision, permittedActions }) => (
            <I18nContext.Provider
              value={{
                lang: decision.language.toLowerCase() as Lang,
                setLang: () => undefined
              }}
            >
              <AssistanceNeedDecisionReadOnly
                decision={decision}
                decisionMakerWarning={
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
                texts={t}
              />
            </I18nContext.Provider>
          )
        )}
      </Content>
      <Gap size="m" />
      <StickyFooter>
        <StickyFooterContainer>
          <FixedSpaceRow justifyContent="space-between" flexWrap="wrap">
            <FixedSpaceRow spacing="s">
              <Button
                onClick={() => navigate(`/reports/assistance-need-decisions`)}
              >
                {t.leavePage}
              </Button>
            </FixedSpaceRow>
            <FixedSpaceRow spacing="m">
              <DangerAsyncButton
                text={i18n.reports.assistanceNeedDecisions.rejectDecision}
                onClick={() => setDecisionModalStatus('REJECTED')}
                onSuccess={() => reloadDecision()}
                data-qa="reject-button"
                disabled={!canBeDecided}
              />
              <AsyncButton
                text={
                  i18n.reports.assistanceNeedDecisions.returnDecisionForEditing
                }
                onClick={() => setDecisionModalStatus('NEEDS_WORK')}
                onSuccess={() => reloadDecision()}
                data-qa="return-for-edit"
                disabled={!canBeDecided}
              />
              <AsyncButton
                primary
                text={i18n.reports.assistanceNeedDecisions.approveDecision}
                onClick={() => setDecisionModalStatus('ACCEPTED')}
                onSuccess={() => reloadDecision()}
                data-qa="approve-button"
                disabled={!canBeDecided}
              />
            </FixedSpaceRow>
          </FixedSpaceRow>
        </StickyFooterContainer>
      </StickyFooter>
    </>
  )
})

type DecisionStatus = Exclude<AssistanceNeedDecisionStatus, 'DRAFT'>

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
  decisionStatus
}: {
  decisionId: UUID
  onClose: (shouldRefresh: boolean) => void
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
          await decideAssistanceNeedDecision(decisionId, decisionStatus)
          onClose(true)
        },
        label: t.okBtn
      }}
    />
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
              await updateAssistanceNeedDecisionDecisionMaker(decisionId, title)
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
