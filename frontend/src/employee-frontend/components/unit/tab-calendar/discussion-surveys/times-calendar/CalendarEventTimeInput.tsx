// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faWarning } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { BoundForm, useFormField, useFormFields } from 'lib-common/form/hooks'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'

import { type calendarEventTimeForm } from '../survey-editor/form'

const InputContainer = styled(FixedSpaceRow)`
  * > input:focus {
    box-shadow: none;
  }
`

export default React.memo(function CalendarEventTimeInput({
  bind
}: {
  bind: BoundForm<typeof calendarEventTimeForm>
}) {
  const timeRange = useFormField(bind, 'timeRange')
  const { startTime, endTime } = useFormFields(timeRange)

  return (
    <InputContainer
      spacing="xxs"
      alignItems="center"
      justifyContent="center"
      data-qa="time-input-container"
    >
      <TimeInputF
        bind={startTime}
        info={{ ...startTime.inputInfo(), text: '' }}
        size="narrow"
        data-qa="event-time-start-input"
      />
      <span>–</span>
      <TimeInputF
        bind={endTime}
        info={{ ...endTime.inputInfo(), text: '' }}
        size="narrow"
        data-qa="event-time-end-input"
      />
      {!timeRange.isValid() && (
        <FontAwesomeIcon icon={faWarning} color={colors.status.warning} />
      )}
    </InputContainer>
  )
})
