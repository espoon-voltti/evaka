// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Decision } from '../../types/decision'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Radio from 'lib-components/atoms/form/Radio'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { useTranslation } from '../../state/i18n'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { acceptDecision, rejectDecision } from '../../api/applications'
import { UUID } from '../../types'

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

  const onSubmit = () => {
    if (accept) {
      return acceptDecision(applicationId, decision.id, acceptDate)
    } else {
      return rejectDecision(applicationId, decision.id)
    }
  }

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
      <AsyncButton
        onClick={onSubmit}
        onSuccess={reloadApplication}
        text={i18n.application.decisions.response.submit}
        primary
        data-qa="decision-send-answer-button"
      />
    </FixedSpaceColumn>
  )
})
