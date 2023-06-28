// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'

import { AssistanceNeedPreschoolDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { DecisionFormReadView } from '../child-information/assistance-need/decision/AssistanceNeedPreschoolDecisionReadPage'
import { assistanceNeedPreschoolDecisionQuery } from '../child-information/queries'

const DecisionView = React.memo(function DecisionView({
  decision: { decision }
}: {
  decision: AssistanceNeedPreschoolDecisionResponse
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
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
          <FixedSpaceRow>{/*todo: action buttons*/}</FixedSpaceRow>
        </FixedSpaceRow>
      </StickyFooter>
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
