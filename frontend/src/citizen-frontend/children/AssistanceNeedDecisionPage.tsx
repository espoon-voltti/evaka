// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Footer from 'citizen-frontend/Footer'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useTranslation } from 'citizen-frontend/localization'
import { LocalizationContext } from 'citizen-frontend/localization/state'
import { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import AssistanceNeedDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedDecisionReadOnly'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { getAssistanceNeedDecision } from './api'

export default React.memo(function AssistanceNeedDecisionPage() {
  const { id } = useNonNullableParams<{ childId: UUID; id: UUID }>()

  const [assistanceNeedDecision] = useApiState(
    () => getAssistanceNeedDecision(id),
    [id]
  )

  const i18n = useTranslation()

  return (
    <>
      <Content>
        <Gap size="m" />
        <ReturnButton label={i18n.common.return} />
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
      texts={i18n.children.assistanceNeed.decisions.decision}
    />
  )
})
