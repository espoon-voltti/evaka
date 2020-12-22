// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { Decision } from 'types/decision'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'components/shared/layout/flex-helpers'
import Radio from 'components/shared/atoms/form/Radio'
import Button from 'components/shared/atoms/buttons/Button'
import { useTranslation } from 'state/i18n'
import { DatePicker } from 'components/common/DatePicker'
import { acceptDecision, rejectDecision } from 'api/applications'
import { UIContext } from 'state/ui'
import { UUID } from 'types'
import LocalDate from '@evaka/lib-common/src/local-date'

interface Props {
  applicationId: UUID
  decision: Decision
  reloadApplication: () => void
  preferredStartDate: LocalDate | null
}

export default React.memo(function DecisionResponse({
  applicationId,
  decision,
  reloadApplication,
  preferredStartDate
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
            type: 'error'
          })
        )
    } else {
      void rejectDecision(applicationId, decision.id)
        .then(() => reloadApplication())
        .catch(() =>
          setErrorMessage({
            title: i18n.common.error.unknown,
            text: i18n.application.decisions.response.rejectError,
            type: 'error'
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
        <DatePicker
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
