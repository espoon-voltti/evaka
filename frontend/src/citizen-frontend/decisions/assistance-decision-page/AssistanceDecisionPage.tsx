// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'

import Footer from 'citizen-frontend/Footer'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useTranslation } from 'citizen-frontend/localization'
import { LocalizationContext } from 'citizen-frontend/localization/state'
import { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import AssistanceNeedDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedDecisionReadOnly'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowDownToLine } from 'lib-icons'

import {
  assistanceDecisionQuery,
  markAssistanceNeedDecisionAsReadMutation
} from './queries'

export default React.memo(function AssistanceNeedDecisionPage() {
  const { id } = useRouteParams(['id'])

  const assistanceNeedDecision = useQueryResult(assistanceDecisionQuery({ id }))
  const i18n = useTranslation()

  const { mutate: markAssistanceNeedDecisionAsRead } = useMutationResult(
    markAssistanceNeedDecisionAsReadMutation
  )
  useEffect(() => {
    markAssistanceNeedDecisionAsRead({ id })
  }, [id, markAssistanceNeedDecisionAsRead])

  return (
    <>
      <Content>
        <Gap size="s" />
        <FixedSpaceRow justifyContent="space-between">
          <ReturnButton label={i18n.common.return} />
          <FixedSpaceRow>
            <LegacyInlineButton
              icon={faArrowDownToLine}
              text={i18n.common.download}
              onClick={() => {
                window.open(
                  `/api/application/citizen/children/assistance-need-decision/${id}/pdf`,
                  '_blank',
                  'noopener,noreferrer'
                )
              }}
              data-qa="assistance-need-decision-download-btn"
              disabled={
                !assistanceNeedDecision.getOrElse(undefined)?.hasDocument
              }
              color={colors.main.m1}
            />
            <Gap horizontal sizeOnMobile="xs" size="zero" />
          </FixedSpaceRow>
        </FixedSpaceRow>
        <Gap size="s" />

        {renderResult(assistanceNeedDecision, (decision) => (
          <LocalizationContext.Provider
            value={{
              lang: decision.language.toLowerCase() as 'fi' | 'sv',
              setLang: () => undefined
            }}
          >
            <DecisionContent decision={decision} />
          </LocalizationContext.Provider>
        ))}

        <Gap size="m" />
        <ReturnButton label={i18n.common.return} />
        <Gap size="s" />
      </Content>
      <Footer />
    </>
  )
})

const DecisionContent = React.memo(function DecisionContent({
  decision
}: {
  decision: AssistanceNeedDecision
}) {
  const i18n = useTranslation()

  return (
    <AssistanceNeedDecisionReadOnly
      decision={decision}
      texts={i18n.decisions.assistanceDecisions.decision}
    />
  )
})
