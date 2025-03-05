// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router'

import { AssistanceNeedPreschoolDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedPreschoolDecisionId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import AssistanceNeedPreschoolDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedPreschoolDecisionReadOnly'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { translations } from 'lib-customizations/employee'
import { faArrowDownToLine } from 'lib-icons'

import { getAssistanceNeedPreschoolDecisionPdf } from '../../../../generated/api-clients/assistanceneed'
import { useTranslation } from '../../../../state/i18n'
import MetadataSection from '../../../archive-metadata/MetadataSection'
import { renderResult } from '../../../async-rendering'
import {
  assistanceNeedPreschoolDecisionMetadataQuery,
  assistanceNeedPreschoolDecisionQuery,
  sendAssistanceNeedPreschoolDecisionMutation,
  unsendAssistanceNeedPreschoolDecisionMutation
} from '../../queries'

const DecisionMetadataSection = React.memo(function DecisionMetadataSection({
  decisionId
}: {
  decisionId: AssistanceNeedPreschoolDecisionId
}) {
  const result = useQueryResult(
    assistanceNeedPreschoolDecisionMetadataQuery({ decisionId })
  )
  return <MetadataSection metadataResult={result} />
})

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
      <Container>
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <ReturnButton label={i18n.common.goBack} />
          {decision.hasDocument && (
            <Button
              appearance="inline"
              text={i18n.common.download}
              icon={faArrowDownToLine}
              onClick={() => {
                window.open(
                  getAssistanceNeedPreschoolDecisionPdf({
                    id: decision.id
                  }).url.toString(),
                  '_blank',
                  'noopener,noreferrer'
                )
              }}
            />
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
      {permittedActions.includes('READ_METADATA') && (
        <>
          <Gap />
          <Container>
            <DecisionMetadataSection decisionId={decision.id} />
          </Container>
        </>
      )}
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
  const decisionId =
    useIdRouteParam<AssistanceNeedPreschoolDecisionId>('decisionId')
  const decisionResult = useQueryResult(
    assistanceNeedPreschoolDecisionQuery({ id: decisionId })
  )

  return renderResult(decisionResult, (decisionResponse) => (
    <DecisionReadView decision={decisionResponse} />
  ))
})
