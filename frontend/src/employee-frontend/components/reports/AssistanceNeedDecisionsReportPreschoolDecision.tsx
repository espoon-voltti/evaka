// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { shade } from 'polished'
import React, { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { AssistanceNeedPreschoolDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { DecisionFormReadView } from '../child-information/assistance-need/decision/AssistanceNeedPreschoolDecisionReadPage'
import { putAssistanceNeedPreschoolDecisionMarkAsOpened } from '../child-information/assistance-need/decision/api-preschool'
import {
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

const DecisionView = React.memo(function DecisionView({
  decision: { decision }
}: {
  decision: AssistanceNeedPreschoolDecisionResponse
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const [confirmationModal, setConfirmationModal] = useState<
    'REJECT' | 'ACCEPT' | null
  >(null)

  const { user } = useContext(UserContext)
  const { refreshAssistanceNeedDecisionCounts } = useContext(
    AssistanceNeedDecisionReportContext
  )
  const { mutateAsync: decide, isLoading: submitting } = useMutationResult(
    decideAssistanceNeedPreschoolDecisionMutation
  )

  const isDecisionMaker =
    user && user.id === decision.form.decisionMakerEmployeeId

  useEffect(() => {
    if (!decision.decisionMakerHasOpened && isDecisionMaker) {
      void putAssistanceNeedPreschoolDecisionMarkAsOpened(decision.id).then(
        () => refreshAssistanceNeedDecisionCounts()
      )
    }
  }, [decision, isDecisionMaker, refreshAssistanceNeedDecisionCounts])

  return (
    <div>
      <DecisionFormReadView decision={decision} />
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
                    status: 'NEEDS_WORK'
                  })
                }
              />
              <Button
                primary
                text={i18n.reports.assistanceNeedDecisions.approveDecision}
                disabled={submitting || !isDecisionMaker}
                onClick={() => setConfirmationModal('ACCEPT')}
              />
            </FixedSpaceRow>
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
              status: 'REJECTED'
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
              status: 'ACCEPTED'
            })
          }
          resolveLabel={i18n.reports.assistanceNeedDecisions.approveModal.okBtn}
          onSuccess={() => setConfirmationModal(null)}
          rejectAction={() => setConfirmationModal(null)}
          rejectLabel={i18n.common.cancel}
        />
      )}
    </div>
  )
})

export default React.memo(
  function AssistanceNeedDecisionsReportPreschoolDecision() {
    const { decisionId } = useNonNullableParams<{ decisionId: UUID }>()
    const decisionResult = useQueryResult(
      assistanceNeedPreschoolDecisionQuery(decisionId)
    )

    return renderResult(decisionResult, (decisionResponse) => (
      <DecisionView decision={decisionResponse} />
    ))
  }
)
