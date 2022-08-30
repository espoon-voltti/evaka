// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { Decision } from 'lib-common/generated/api-types/decision'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'

import { acceptDecision, rejectDecision } from '../../api/applications'
import { useTranslation } from '../../state/i18n'

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
      return acceptDecision(applicationId, decision.id, acceptDate)
    } else {
      return rejectDecision(applicationId, decision.id)
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
          id="decision-radio-accept"
        />
        <DatePicker
          data-qa="decision-start-date-picker"
          date={acceptDate}
          onChange={(date) => date && setAcceptDate(date)}
          minDate={decision.startDate.subMonths(1)}
          maxDate={decision.startDate.addWeeks(2)}
          labels={i18n.common.datePicker}
          errorTexts={i18n.validationErrors}
          aria-labelledby="decision-radio-accept"
          locale="fi"
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
