// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faCheck, faTrash } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { requiredLocalTimeRange } from 'lib-common/form/fields'
import { object, value, required, mapped } from 'lib-common/form/form'
import { useForm, useFormField, useFormFields } from 'lib-common/form/hooks'
import { CalendarEventTimeForm } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

const InputContainer = styled(FixedSpaceRow)`
  margin-left: 10px;
  * > input:focus {
    box-shadow: none;
  }
`
const newCalendarEventTimeForm = object({
  date: required(value<LocalDate>()),
  timeRange: required(requiredLocalTimeRange())
})

export default React.memo(function NewCalendarEventTimeEditor({
  addAction,
  removeAction,
  editorId,
  day
}: {
  addAction: (et: CalendarEventTimeForm) => void
  removeAction: (id: UUID) => void
  editorId: UUID
  day: LocalDate
}) {
  const { i18n } = useTranslation()

  const mappedForm = mapped(newCalendarEventTimeForm, (output) => ({
    date: output.date,
    startTime: output.timeRange.start,
    endTime: output.timeRange.end
  }))

  const eventTimeForm = useForm(
    mappedForm,
    () => ({
      date: day,
      timeRange: {
        startTime: '',
        endTime: ''
      }
    }),
    i18n.validationErrors
  )

  const timeRange = useFormField(eventTimeForm, 'timeRange')
  const { startTime, endTime } = useFormFields(timeRange)

  return (
    <InputContainer spacing="xxs" alignItems="center" justifyContent="center">
      <TimeInputF bind={startTime} info={{ text: '' }} />
      <span>–</span>
      <TimeInputF bind={endTime} info={{ text: '' }} />
      <InlineButton
        icon={faCheck}
        disabled={!eventTimeForm.isValid()}
        text=""
        onClick={() => {
          addAction(eventTimeForm.value())
        }}
      />
      <InlineButton
        icon={faTrash}
        text=""
        onClick={() => {
          removeAction(editorId)
        }}
      />
    </InputContainer>
  )
})
