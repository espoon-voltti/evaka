// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { localTimeRange } from 'lib-common/form/fields'
import { BoundFormShape, useFormField } from 'lib-common/form/hooks'
import { ShapeOf, StateOf } from 'lib-common/form/types'
import UnderRowStatusIcon, { InfoStatus } from 'lib-components/atoms/StatusIcon'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../localization'

export interface Props {
  bind: BoundFormShape<
    StateOf<typeof localTimeRange>,
    ShapeOf<typeof localTimeRange>
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
    <div>
      <TimeRangeWrapper>
        <TimeInputF
          wide
          bind={startTime}
          placeholder={i18n.calendar.reservationModal.start}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          onFocus={onFocus}
          onBlur={() => setTouched(([_, t]) => [true, t])}
          data-qa={dataQa ? `${dataQa}-start` : undefined}
        />
        <span>–</span>
        <TimeInputF
          wide
          bind={endTime}
          placeholder={i18n.calendar.reservationModal.end}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          onFocus={onFocus}
          onBlur={() => setTouched(([t, _]) => [t, true])}
          data-qa={dataQa ? `${dataQa}-end` : undefined}
        />
      </TimeRangeWrapper>
      {inputInfo !== undefined && (!hideErrorsBeforeTouched || bothTouched) ? (
        <ErrorRow $status={inputInfo.status}>
          <span data-qa={dataQa ? `${dataQa}-info` : undefined}>
            {inputInfo.text}
          </span>
          <UnderRowStatusIcon status={inputInfo?.status} />
        </ErrorRow>
      ) : undefined}
    </div>
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

const ErrorRow = styled.div<{ $status: InfoStatus | undefined }>`
  font-size: 1rem;
  line-height: 1rem;
  margin-top: ${defaultMargins.xxs};

  color: ${(p) =>
    p.$status === 'success'
      ? p.theme.colors.accents.a1greenDark
      : p.$status === 'warning'
      ? p.theme.colors.accents.a2orangeDark
      : p.theme.colors.grayscale.g70};
`
