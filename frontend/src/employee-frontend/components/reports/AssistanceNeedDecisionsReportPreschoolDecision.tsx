// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { shade } from 'polished'
import React, { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { string } from 'lib-common/form/fields'
import { object, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import {
  AssistanceNeedPreschoolDecision,
  AssistanceNeedPreschoolDecisionResponse
} from 'lib-common/generated/api-types/assistanceneed'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import AssistanceNeedPreschoolDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedPreschoolDecisionReadOnly'
import Button from 'lib-components/atoms/buttons/Button'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  AsyncFormModal,
  MutateFormModal
} from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { translations } from 'lib-customizations/employee'

import { markAssistanceNeedDecisionAsOpened } from '../../generated/api-clients/assistanceneed'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import {
  annulAssistanceNeedPreschoolDecisionMutation,
  assistanceNeedPreschoolDecisionQuery,
  decideAssistanceNeedPreschoolDecisionMutation
} from '../child-information/queries'

import { AssistanceNeedDecisionReportContext } from './AssistanceNeedDecisionReportContext'

const DangerButton = styled(Button)`
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
    AssistanceNeedDecisionReportContext
  )
  const { mutateAsync: decide, isPending: submitting } = useMutationResult(
    decideAssistanceNeedPreschoolDecisionMutation
  )

  const isDecisionMaker =
    user && user.id === decision.form.decisionMakerEmployeeId

  useEffect(() => {
    if (!decision.decisionMakerHasOpened && isDecisionMaker) {
      void markAssistanceNeedDecisionAsOpened({ id: decision.id }).then(() =>
        refreshAssistanceNeedDecisionCounts()
      )
    }
  }, [decision, isDecisionMaker, refreshAssistanceNeedDecisionCounts])

  return (
    <div>
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
          <Button
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
              <Button
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
              <Button
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
    const { decisionId } = useNonNullableParams<{ decisionId: UUID }>()
    const decisionResult = useQueryResult(
      assistanceNeedPreschoolDecisionQuery({ id: decisionId })
    )

    return renderResult(decisionResult, (decisionResponse) => (
      <DecisionView decision={decisionResponse} />
    ))
  }
)
