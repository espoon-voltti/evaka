// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { wrapResult } from 'lib-common/api'
import { Decision } from 'lib-common/generated/api-types/decision'
import { UUID } from 'lib-common/types'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import {
  acceptDecision,
  rejectDecision
} from '../../generated/api-clients/application'
import { useTranslation } from '../../state/i18n'

const acceptDecisionResult = wrapResult(acceptDecision)
const rejectDecisionResult = wrapResult(rejectDecision)

interface Props {
  applicationId: UUID
  decision: Decision
  reloadApplication: () => void
}

export default React.memo(function DecisionResponse({
  applicationId,
  decision,
  reloadApplication
}: Props) {
  const { i18n } = useTranslation()
  const [accept, setAccept] = useState(true)
  const [acceptDate, setAcceptDate] = useState(decision.startDate)

  const onSubmit = useCallback(() => {
    if (accept) {
      return acceptDecisionResult({
        applicationId,
        body: { decisionId: decision.id, requestedStartDate: acceptDate }
      })
    } else {
      return rejectDecisionResult({
        applicationId,
        body: { decisionId: decision.id }
      })
    }
  }, [accept, acceptDate, applicationId, decision.id])

  return (
    <FixedSpaceColumn>
      <FixedSpaceRow>
        <Radio
          data-qa="decision-radio-accept"
          checked={accept}
          label={i18n.application.decisions.response.accept}
          onChange={() => setAccept(true)}
        />
        <DatePickerDeprecated
          data-qa="decision-start-date-picker"
          type="short"
          date={acceptDate}
          onChange={setAcceptDate}
          minDate={decision.startDate.subMonths(1)}
          maxDate={decision.startDate.addWeeks(2)}
        />
      </FixedSpaceRow>
      <Radio
        data-qa="decision-radio-reject"
        checked={!accept}
        label={i18n.application.decisions.response.reject}
        onChange={() => setAccept(false)}
      />
      <LegacyAsyncButton
        onClick={onSubmit}
        onSuccess={reloadApplication}
        text={i18n.application.decisions.response.submit}
        primary
        data-qa="decision-send-answer-button"
      />
    </FixedSpaceColumn>
  )
})
