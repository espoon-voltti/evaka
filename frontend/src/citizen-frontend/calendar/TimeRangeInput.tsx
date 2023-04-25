// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React, { useState } from 'react'
import styled from 'styled-components'

import type { BoundFormShape } from 'lib-common/form/hooks'
import { useFormField } from 'lib-common/form/hooks'
import type { Form } from 'lib-common/form/types'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'

import { useTranslation } from '../localization'

export interface Props {
  bind: BoundFormShape<
    { startTime: string; endTime: string },
    {
      startTime: Form<unknown, string, string, unknown>
      endTime: Form<unknown, string, string, unknown>
    }
  >
  hideErrorsBeforeTouched?: boolean
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
  'data-qa'?: string
}

export default React.memo(function TimeRangeInputF({
  bind,
  hideErrorsBeforeTouched,
  onFocus,
  'data-qa': dataQa
}: Props) {
  const i18n = useTranslation()
  const [touched, setTouched] = useState([false, false])
  const bothTouched = touched[0] && touched[1]

  const startTime = useFormField(bind, 'startTime')
  const endTime = useFormField(bind, 'endTime')
  const inputInfo = bind.inputInfo()

  return (
    <>
      <TimeRangeWrapper>
        <TimeInputF
          bind={startTime}
          placeholder={i18n.calendar.reservationModal.start}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          onFocus={onFocus}
          onBlur={() => setTouched(([_, t]) => [true, t])}
          data-qa={dataQa ? `${dataQa}-start` : undefined}
        />
        <span>–</span>
        <TimeInputF
          bind={endTime}
          placeholder={i18n.calendar.reservationModal.end}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          onFocus={onFocus}
          onBlur={() => setTouched(([t, _]) => [t, true])}
          data-qa={dataQa ? `${dataQa}-end` : undefined}
        />
      </TimeRangeWrapper>
      {inputInfo !== undefined && (!hideErrorsBeforeTouched || bothTouched) ? (
        <InputFieldUnderRow className={classNames(inputInfo.status)}>
          <span data-qa={dataQa ? `${dataQa}-info` : undefined}>
            {inputInfo.text}
          </span>
          <UnderRowStatusIcon status={inputInfo?.status} />
        </InputFieldUnderRow>
      ) : undefined}
    </>
  )
})

const TimeRangeWrapper = styled.div`
  display: grid;
  grid-template-columns: 5fr 1fr 5fr;

  & > :nth-child(2) {
    display: flex;
    justify-content: center;
    padding-top: 8px;
  }
`
