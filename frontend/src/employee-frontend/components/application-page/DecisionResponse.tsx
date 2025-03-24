// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Decision } from 'lib-common/generated/api-types/decision'
import { ApplicationId } from 'lib-common/generated/api-types/shared'
import { first, second, useSelectMutation } from 'lib-common/query'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import { useTranslation } from '../../state/i18n'

import { acceptDecisionMutation, rejectDecisionMutation } from './queries'

interface Props {
  applicationId: ApplicationId
  decision: Decision
}

export default React.memo(function DecisionResponse({
  applicationId,
  decision
}: Props) {
  const { i18n } = useTranslation()
  const [accept, setAccept] = useState(true)
  const [acceptDate, setAcceptDate] = useState(decision.startDate)

  const [mutation, onClick] = useSelectMutation(
    () => (accept ? first() : second()),
    [
      acceptDecisionMutation,
      () => ({
        applicationId,
        body: { decisionId: decision.id, requestedStartDate: acceptDate }
      })
    ],
    [
      rejectDecisionMutation,
      () => ({
        applicationId,
        body: { decisionId: decision.id }
      })
    ]
  )

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
      <MutateButton
        mutation={mutation}
        onClick={onClick}
        text={i18n.application.decisions.response.submit}
        primary
        data-qa="decision-send-answer-button"
      />
    </FixedSpaceColumn>
  )
})
