// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { Decision } from 'types/decision'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { useTranslation } from 'state/i18n'
import { DatePickerDeprecated } from '@evaka/lib-components/src/molecules/DatePickerDeprecated'
import { acceptDecision, rejectDecision } from 'api/applications'
import { UIContext } from 'state/ui'
import { UUID } from 'types'

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
  const { setErrorMessage } = useContext(UIContext)
  const [accept, setAccept] = useState(true)
  const [acceptDate, setAcceptDate] = useState(decision.startDate)

  const onSubmit = () => {
    if (accept) {
      void acceptDecision(applicationId, decision.id, acceptDate)
        .then(() => reloadApplication())
        .catch(() =>
          setErrorMessage({
            title: i18n.common.error.unknown,
            text: i18n.application.decisions.response.acceptError,
            type: 'error',
            resolveLabel: i18n.common.ok
          })
        )
    } else {
      void rejectDecision(applicationId, decision.id)
        .then(() => reloadApplication())
        .catch(() =>
          setErrorMessage({
            title: i18n.common.error.unknown,
            text: i18n.application.decisions.response.rejectError,
            type: 'error',
            resolveLabel: i18n.common.ok
          })
        )
    }
  }

  return (
    <FixedSpaceColumn>
      <FixedSpaceRow>
        <Radio
          dataQa="decision-radio-accept"
          checked={accept}
          label={i18n.application.decisions.response.accept}
          onChange={() => setAccept(true)}
        />
        <DatePickerDeprecated
          dataQa="decision-start-date-picker"
          type="short"
          date={acceptDate}
          onChange={setAcceptDate}
          minDate={decision.startDate.subMonths(1)}
          maxDate={decision.startDate.addWeeks(2)}
        />
      </FixedSpaceRow>
      <Radio
        dataQa="decision-radio-reject"
        checked={!accept}
        label={i18n.application.decisions.response.reject}
        onChange={() => setAccept(false)}
      />
      <Button
        dataQa="decision-send-answer-button"
        onClick={onSubmit}
        text={i18n.application.decisions.response.submit}
        primary
      />
    </FixedSpaceColumn>
  )
})
