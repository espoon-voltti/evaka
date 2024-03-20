// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faCheck, faTrash } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import { BoundForm, useFormField, useFormFields } from 'lib-common/form/hooks'
import { CalendarEventTimeForm } from 'lib-common/generated/api-types/calendarevent'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { calendarEventTimeForm } from '../survey-editor/form'

const InputContainer = styled(FixedSpaceRow)`
  margin-left: 10px;
  * > input:focus {
    box-shadow: none;
  }
`
export default React.memo(function NewCalendarEventTimeEditor({
  addAction,
  removeAction,
  bind
}: {
  addAction: (et: CalendarEventTimeForm) => void
  removeAction: (id: UUID) => void
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
        info={{ text: '' }}
        size="narrow"
        data-qa="event-time-start-input"
      />
      <span>–</span>
      <TimeInputF
        bind={endTime}
        info={{ text: '' }}
        size="narrow"
        data-qa="event-time-end-input"
      />
      <InlineButton
        icon={faCheck}
        disabled={!bind.isValid()}
        text=""
        onClick={() => {
          addAction(bind.value())
        }}
        data-qa="event-time-submit"
      />
      <InlineButton
        icon={faTrash}
        text=""
        onClick={() => {
          removeAction(bind.state.id)
        }}
        data-qa="event-time-delete"
      />
    </InputContainer>
  )
})
