// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { shade } from 'polished'
import React, { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { string } from 'lib-common/form/fields'
import { object, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import type {
  AssistanceNeedPreschoolDecision,
  AssistanceNeedPreschoolDecisionResponse
} from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import AssistanceNeedPreschoolDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedPreschoolDecisionReadOnly'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Container from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  AsyncFormModal,
  MutateFormModal
} from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { translations } from 'lib-customizations/employee'
import { faArrowDownToLine } from 'lib-icons'

import {
  getAssistanceNeedPreschoolDecisionPdf,
  markAssistanceNeedPreschoolDecisionAsOpened
} from '../../generated/api-clients/assistanceneed'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import {
  annulAssistanceNeedPreschoolDecisionMutation,
  assistanceNeedPreschoolDecisionQuery,
  decideAssistanceNeedPreschoolDecisionMutation
} from '../child-information/queries'

import { ReportNotificationContext } from './ReportNotificationContext'

const DangerButton = styled(LegacyButton)`
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

const annulForm = object({
  reason: validated(string(), nonBlank)
})

const AnnulModal = React.memo(function AnnulModal({
  decision,
  onClose
}: {
  decision: AssistanceNeedPreschoolDecision
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const boundForm = useForm(
    annulForm,
    () => ({ reason: '' }),
    i18n.validationErrors
  )
  const { reason } = useFormFields(boundForm)

  return (
    <MutateFormModal
      title={i18n.reports.assistanceNeedDecisions.annulModal.title}
      text={i18n.reports.assistanceNeedDecisions.annulModal.text}
      resolveLabel={i18n.reports.assistanceNeedDecisions.annulModal.okBtn}
      resolveMutation={annulAssistanceNeedPreschoolDecisionMutation}
      resolveAction={() => ({
        id: decision.id,
        childId: decision.child.id,
        body: {
          reason: reason.value()
        }
      })}
      onSuccess={onClose}
      rejectLabel={i18n.common.cancel}
      rejectAction={onClose}
      resolveDisabled={!boundForm.isValid()}
    >
      <InputFieldF bind={reason} data-qa="annul-reason-input" />
    </MutateFormModal>
  )
})

const DecisionView = React.memo(function DecisionView({
  decision: { decision }
}: {
  decision: AssistanceNeedPreschoolDecisionResponse
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const [confirmationModal, setConfirmationModal] = useState<
    'REJECT' | 'ACCEPT' | 'ANNUL' | null
  >(null)

  const { user } = useContext(UserContext)
  const { refreshAssistanceNeedDecisionCounts } = useContext(
    ReportNotificationContext
  )
  const { mutateAsync: decide, isPending: submitting } = useMutationResult(
    decideAssistanceNeedPreschoolDecisionMutation
  )

  const isDecisionMaker =
    user && user.id === decision.form.decisionMakerEmployeeId

  useEffect(() => {
    if (!decision.decisionMakerHasOpened && isDecisionMaker) {
      void markAssistanceNeedPreschoolDecisionAsOpened({
        id: decision.id
      }).then(() => refreshAssistanceNeedDecisionCounts())
    }
  }, [decision, isDecisionMaker, refreshAssistanceNeedDecisionCounts])

  return (
    <div>
      <Container>
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <ReturnButton label={i18n.common.goBack} />
          {decision.hasDocument && (
            <a
              href={getAssistanceNeedPreschoolDecisionPdf({
                id: decision.id
              }).url.toString()}
              target="_blank"
              rel="noreferrer"
            >
              <Button
                appearance="inline"
                icon={faArrowDownToLine}
                text={i18n.common.download}
                onClick={() => undefined}
              />
            </a>
          )}
        </FixedSpaceRow>
      </Container>
      <AssistanceNeedPreschoolDecisionReadOnly
        decision={decision}
        texts={
          translations[decision.form.language === 'SV' ? 'sv' : 'fi']
            .childInformation.assistanceNeedPreschoolDecision
        }
      />
      <Gap size="m" />
      <StickyFooter>
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <LegacyButton
            text={i18n.childInformation.assistanceNeedDecision.leavePage}
            onClick={() => navigate(`/reports/assistance-need-decisions`)}
          />

          {['DRAFT', 'NEEDS_WORK'].includes(decision.status) && (
            <FixedSpaceRow>
              <DangerButton
                text={i18n.reports.assistanceNeedDecisions.rejectDecision}
                disabled={submitting || !isDecisionMaker}
                onClick={() => setConfirmationModal('REJECT')}
                data-qa="reject-button"
              />
              <LegacyButton
                text={
                  i18n.reports.assistanceNeedDecisions.returnDecisionForEditing
                }
                disabled={submitting || !isDecisionMaker}
                onClick={() =>
                  decide({
                    childId: decision.child.id,
                    id: decision.id,
                    body: {
                      status: 'NEEDS_WORK'
                    }
                  })
                }
                data-qa="return-for-edit-button"
              />
              <LegacyButton
                primary
                text={i18n.reports.assistanceNeedDecisions.approveDecision}
                disabled={submitting || !isDecisionMaker}
                onClick={() => setConfirmationModal('ACCEPT')}
                data-qa="approve-button"
              />
            </FixedSpaceRow>
          )}

          {decision.status === 'ACCEPTED' && (
            <DangerButton
              text={i18n.reports.assistanceNeedDecisions.annulDecision}
              disabled={submitting || !isDecisionMaker}
              onClick={() => setConfirmationModal('ANNUL')}
              data-qa="annul-button"
            />
          )}
        </FixedSpaceRow>
      </StickyFooter>

      {confirmationModal === 'REJECT' && (
        <AsyncFormModal
          title={i18n.reports.assistanceNeedDecisions.rejectModal.title}
          text={i18n.reports.assistanceNeedDecisions.rejectModal.text}
          resolveAction={() =>
            decide({
              childId: decision.child.id,
              id: decision.id,
              body: {
                status: 'REJECTED'
              }
            })
          }
          resolveLabel={i18n.reports.assistanceNeedDecisions.rejectModal.okBtn}
          onSuccess={() => setConfirmationModal(null)}
          rejectAction={() => setConfirmationModal(null)}
          rejectLabel={i18n.common.cancel}
        />
      )}

      {confirmationModal === 'ACCEPT' && (
        <AsyncFormModal
          title={i18n.reports.assistanceNeedDecisions.approveModal.title}
          text={i18n.reports.assistanceNeedDecisions.approveModal.text}
          resolveAction={() =>
            decide({
              childId: decision.child.id,
              id: decision.id,
              body: {
                status: 'ACCEPTED'
              }
            })
          }
          resolveLabel={i18n.reports.assistanceNeedDecisions.approveModal.okBtn}
          onSuccess={() => setConfirmationModal(null)}
          rejectAction={() => setConfirmationModal(null)}
          rejectLabel={i18n.common.cancel}
        />
      )}

      {confirmationModal === 'ANNUL' && (
        <AnnulModal
          decision={decision}
          onClose={() => setConfirmationModal(null)}
        />
      )}
    </div>
  )
})

export default React.memo(
  function AssistanceNeedDecisionsReportPreschoolDecision() {
    const decisionId =
      useIdRouteParam<AssistanceNeedPreschoolDecisionId>('decisionId')
    const decisionResult = useQueryResult(
      assistanceNeedPreschoolDecisionQuery({ id: decisionId })
    )

    return renderResult(decisionResult, (decisionResponse) => (
      <DecisionView decision={decisionResponse} />
    ))
  }
)
