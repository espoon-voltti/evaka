// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { I18nContext, Lang, useTranslation } from 'employee-frontend/state/i18n'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content, { ContentArea } from 'lib-components/layout/Container'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, InformationText } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import AssistanceNeedDecisionReadOnly from './AssistanceNeedDecisionReadOnly'
import { getAssistanceNeedDecision, sendAssistanceNeedDecision } from './api'

const StickyFooterContainer = styled.div`
  padding: ${defaultMargins.xs};
`

export default React.memo(function AssistanceNeedDecisionPage() {
  const { childId, id } = useNonNullableParams<{ childId: UUID; id: UUID }>()
  const navigate = useNavigate()

  const [assistanceNeedDecision, reloadDecision] = useApiState(
    () => getAssistanceNeedDecision(id),
    [id]
  )

  const {
    i18n: {
      childInformation: { assistanceNeedDecision: t },
      ...i18n
    }
  } = useTranslation()

  const [appealInstructionsOpen, setAppealInstructionsOpen] = useState(false)

  const canBeEdited = assistanceNeedDecision
    .map(
      ({ decision }) =>
        decision.status === 'NEEDS_WORK' ||
        (decision.status === 'DRAFT' && decision.sentForDecision === null)
    )
    .getOrElse(false)

  return (
    <>
      <Content>
        <ReturnButton label={i18n.common.goBack} />

        <ContentArea opaque>
          {renderResult(assistanceNeedDecision, ({ decision }) => (
            <I18nContext.Provider
              value={{
                lang: decision.language.toLowerCase() as Lang,
                setLang: () => undefined
              }}
            >
              <AssistanceNeedDecisionReadOnly decision={decision} />
            </I18nContext.Provider>
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
          {renderResult(
            assistanceNeedDecision,
            ({ decision, permittedActions }) => (
              <FixedSpaceRow justifyContent="space-between" flexWrap="wrap">
                <FixedSpaceRow spacing="s">
                  <Button
                    onClick={() => navigate(`/child-information/${childId}`)}
                  >
                    {t.leavePage}
                  </Button>
                  {permittedActions.includes('UPDATE') && (
                    <Button
                      onClick={() =>
                        navigate(
                          `/child-information/${childId}/assistance-need-decision/${id}/edit`
                        )
                      }
                      disabled={!canBeEdited}
                    >
                      {t.modifyDecision}
                    </Button>
                  )}
                </FixedSpaceRow>
                <FixedSpaceRow spacing="m">
                  {decision.sentForDecision && (
                    <FixedSpaceColumn
                      justifyContent="center"
                      alignItems="flex-end"
                      spacing="none"
                    >
                      <InformationText>{t.sentToDecisionMaker}</InformationText>
                      <InformationText data-qa="decision-sent-at">
                        {decision.sentForDecision.format()}
                      </InformationText>
                    </FixedSpaceColumn>
                  )}

                  {permittedActions.includes('SEND') && (
                    <AsyncButton
                      primary
                      text={t.sendToDecisionMaker}
                      onClick={() => sendAssistanceNeedDecision(id)}
                      onSuccess={reloadDecision}
                      disabled={!canBeEdited}
                      data-qa="send-decision"
                    />
                  )}
                </FixedSpaceRow>
              </FixedSpaceRow>
            ),
            { size: 'L', margin: 'zero' }
          )}
        </StickyFooterContainer>
      </StickyFooter>
    </>
  )
})
