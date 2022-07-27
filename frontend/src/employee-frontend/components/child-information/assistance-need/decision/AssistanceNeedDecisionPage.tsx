// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import type { Lang } from 'employee-frontend/state/i18n'
import { I18nContext, useTranslation } from 'employee-frontend/state/i18n'
import type { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import type { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import AssistanceNeedDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedDecisionReadOnly'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { InformationText } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

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

  useEffect(() => {
    if (assistanceNeedDecision.getOrElse(undefined)?.hasMissingFields) {
      navigate(
        `/child-information/${childId}/assistance-need-decision/${id}/edit`,
        {
          replace: true
        }
      )
    }
  }, [assistanceNeedDecision, childId, id, navigate])

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

        {renderResult(assistanceNeedDecision, ({ decision }) => (
          <I18nContext.Provider
            value={{
              lang: decision.language.toLowerCase() as Lang,
              setLang: () => undefined
            }}
          >
            <AssistanceNeedDecisionContent decision={decision} />
          </I18nContext.Provider>
        ))}
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

const AssistanceNeedDecisionContent = React.memo(
  function AssistanceNeedDecisionContent({
    decision
  }: {
    decision: AssistanceNeedDecision
  }) {
    const {
      i18n: {
        childInformation: { assistanceNeedDecision: t }
      }
    } = useTranslation()

    return <AssistanceNeedDecisionReadOnly decision={decision} texts={t} />
  }
)
