// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'

import { AssistanceNeedPreschoolDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import AssistanceNeedPreschoolDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedPreschoolDecisionReadOnly'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { translations } from 'lib-customizations/employee'

import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import {
  assistanceNeedPreschoolDecisionQuery,
  sendAssistanceNeedPreschoolDecisionMutation,
  unsendAssistanceNeedPreschoolDecisionMutation
} from '../../queries'

const DecisionReadView = React.memo(function DecisionReadView({
  decision: { decision, permittedActions }
}: {
  decision: AssistanceNeedPreschoolDecisionResponse
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { mutateAsync: sendForDecision } = useMutationResult(
    sendAssistanceNeedPreschoolDecisionMutation
  )
  const { mutateAsync: revertSendForDecision } = useMutationResult(
    unsendAssistanceNeedPreschoolDecisionMutation
  )

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
          <FixedSpaceRow alignItems="center">
            <LegacyButton
              text={i18n.childInformation.assistanceNeedDecision.leavePage}
              onClick={() =>
                navigate(`/child-information/${decision.child.id}`)
              }
              data-qa="leave-page-button"
            />
            <LegacyButton
              text={i18n.childInformation.assistanceNeedDecision.modifyDecision}
              disabled={
                decision.status !== 'NEEDS_WORK' &&
                !(decision.status === 'DRAFT' && !decision.sentForDecision)
              }
              onClick={() =>
                navigate(
                  `/child-information/${decision.child.id}/assistance-need-preschool-decisions/${decision.id}/edit`
                )
              }
              data-qa="edit-button"
            />
          </FixedSpaceRow>

          <FixedSpaceRow>
            {decision.sentForDecision && (
              <FixedSpaceColumn spacing="xs">
                <span>
                  {
                    i18n.childInformation.assistanceNeedDecision
                      .sentToDecisionMaker
                  }
                </span>
                <span>{decision.sentForDecision.format()}</span>
              </FixedSpaceColumn>
            )}
            {permittedActions.includes('REVERT_TO_UNSENT') &&
              decision.sentForDecision !== null &&
              ['DRAFT', 'NEEDS_WORK'].includes(decision.status) && (
                <AsyncButton
                  text={
                    i18n.childInformation.assistanceNeedDecision.revertToUnsent
                  }
                  onClick={() =>
                    revertSendForDecision({
                      childId: decision.child.id,
                      id: decision.id
                    })
                  }
                  onSuccess={() => undefined}
                  data-qa="revert-to-unsent"
                />
              )}
            {permittedActions.includes('SEND') &&
              (decision.status === 'NEEDS_WORK' ||
                (decision.status === 'DRAFT' &&
                  decision.sentForDecision === null)) && (
                <AsyncButton
                  primary
                  text={
                    i18n.childInformation.assistanceNeedDecision
                      .sendToDecisionMaker
                  }
                  disabled={!decision.isValid}
                  onClick={() =>
                    sendForDecision({
                      childId: decision.child.id,
                      id: decision.id
                    })
                  }
                  onSuccess={() => undefined}
                  data-qa="send-decision"
                />
              )}
          </FixedSpaceRow>
        </FixedSpaceRow>
      </StickyFooter>
    </div>
  )
})

export default React.memo(function AssistanceNeedPreschoolDecisionReadPage() {
  const { decisionId } = useRouteParams(['decisionId'])
  const decisionResult = useQueryResult(
    assistanceNeedPreschoolDecisionQuery({ id: decisionId })
  )

  return renderResult(decisionResult, (decisionResponse) => (
    <DecisionReadView decision={decisionResponse} />
  ))
})
