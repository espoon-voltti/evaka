// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { shade } from 'polished'
import React, { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UserContext } from 'employee-frontend/state/user'
import { Failure } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content, { ContentArea } from 'lib-components/layout/Container'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import AssistanceNeedDecisionReadOnly from '../child-information/assistance-need/decision/AssistanceNeedDecisionReadOnly'
import {
  decideAssistanceNeedDecision,
  getAssistanceNeedDecision,
  markAssistanceNeedDecisionAsOpened
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

  const [appealInstructionsOpen, setAppealInstructionsOpen] = useState(false)

  const [returnForEdit, setReturnForEdit] = useState(false)

  return (
    <>
      {!!returnForEdit && (
        <ReturnForEditDecisionModal
          onClose={(shouldRefresh) => {
            setReturnForEdit(false)
            if (shouldRefresh) {
              reloadDecision()
            }
          }}
          decisionId={id}
        />
      )}

      <Content>
        <ReturnButton label={i18n.common.goBack} />

        <ContentArea opaque>
          {renderResult(assistanceNeedDecision, ({ decision }) => (
            <AssistanceNeedDecisionReadOnly decision={decision} />
          ))}
        </ContentArea>
        <Gap size="m" />
        <CollapsibleContentArea
          title={<H2 noMargin>{t.appealInstructionsTitle}</H2>}
          open={appealInstructionsOpen}
          toggleOpen={() => setAppealInstructionsOpen(!appealInstructionsOpen)}
          opaque
        >
          {t.appealInstructions}
        </CollapsibleContentArea>
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
            {renderResult(
              assistanceNeedDecision,
              ({ permittedActions }) =>
                !permittedActions.includes('DECIDE') ? null : (
                  <FixedSpaceRow spacing="m">
                    <DangerAsyncButton
                      text="Hylkää päätös"
                      onClick={() =>
                        Promise.resolve(Failure.of({ message: '' }))
                      }
                      onSuccess={() => reloadDecision()}
                    />
                    <AsyncButton
                      text="Palauta korjattavaksi"
                      onClick={() => setReturnForEdit(true)}
                      onSuccess={() => reloadDecision()}
                      data-qa="return-for-edit"
                    />
                    <AsyncButton
                      primary
                      text="Hyväksy päätös"
                      onClick={() =>
                        Promise.resolve(Failure.of({ message: '' }))
                      }
                      onSuccess={() => reloadDecision()}
                    />
                  </FixedSpaceRow>
                ),
              { size: 'L', margin: 'zero' }
            )}
          </FixedSpaceRow>
        </StickyFooterContainer>
      </StickyFooter>
    </>
  )
})

const ReturnForEditDecisionModal = React.memo(function DeleteAbsencesModal({
  decisionId,
  onClose
}: {
  decisionId: UUID
  onClose: (shouldRefresh: boolean) => void
}) {
  const { i18n } = useTranslation()
  return (
    <InfoModal
      type="warning"
      title={i18n.reports.assistanceNeedDecisions.returnForEditModal.title}
      icon={faQuestion}
      reject={{
        action: () => onClose(false),
        label: i18n.common.cancel
      }}
      resolve={{
        async action() {
          await decideAssistanceNeedDecision(decisionId, 'NEEDS_WORK')
          onClose(true)
        },
        label:
          i18n.reports.assistanceNeedDecisions.returnForEditModal.returnForEdit
      }}
    />
  )
})
