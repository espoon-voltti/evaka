// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { BoundForm, useFormField, useFormFields } from 'lib-common/form/hooks'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { calendarEventTimeForm } from '../survey-editor/DiscussionSurveyEditor'

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
    <InputContainer spacing="xxs" alignItems="center" justifyContent="center">
      <TimeInputF bind={startTime} info={{ text: '' }} />
      <span>–</span>
      <TimeInputF bind={endTime} info={{ text: '' }} />
    </InputContainer>
  )
})
