// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import { useFormField, BoundFormShape } from 'lib-common/form/hooks'
import { Form } from 'lib-common/form/types'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'

import { useTranslation } from '../localization'

export default React.memo(function TimeRangeInputF({
  bind,
  'data-qa': dataQa
}: {
  bind: BoundFormShape<
    { startTime: string; endTime: string },
    {
      startTime: Form<unknown, string, string, unknown>
      endTime: Form<unknown, string, string, unknown>
    }
  >
  'data-qa'?: string
}) {
  const i18n = useTranslation()

  const startTime = useFormField(bind, 'startTime')
  const endTime = useFormField(bind, 'endTime')
  const inputInfo = bind.inputInfo()

  return (
    <>
      <TimeRangeWrapper>
        <TimeInputF
          bind={startTime}
          placeholder={i18n.calendar.reservationModal.start}
          data-qa={dataQa ? `${dataQa}-start` : undefined}
        />
        <span>–</span>
        <TimeInputF
          bind={endTime}
          placeholder={i18n.calendar.reservationModal.end}
          data-qa={dataQa ? `${dataQa}-end` : undefined}
        />
      </TimeRangeWrapper>
      {inputInfo !== undefined ? (
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
