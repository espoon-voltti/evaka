// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { ErrorKey } from 'lib-common/form-validation'
import TimeInput from 'lib-components/atoms/form/TimeInput'

import { errorToInputInfo } from '../input-info-helper'
import { useTranslation } from '../localization'

export interface TimeRangeWithErrors {
  startTime: string
  endTime: string
  errors: { startTime: ErrorKey | undefined; endTime: ErrorKey | undefined }
}

export default React.memo(function TimeRangeInput({
  value,
  onChange,
  'data-qa': dataQa
}: {
  value: TimeRangeWithErrors
  onChange: (field: 'startTime' | 'endTime') => (value: string) => void
  'data-qa'?: string
}) {
  const i18n = useTranslation()
  const { startTime, endTime, errors } = value

  // These are actually callbacks, but they are created by the onChange function
  // call, so useMemo works better
  const onChangeStart = useMemo(() => onChange('startTime'), [onChange])
  const onChangeEnd = useMemo(() => onChange('endTime'), [onChange])

  return (
    <>
      <TimeInput
        value={startTime}
        onChange={onChangeStart}
        info={errorToInputInfo(errors.startTime, i18n.validationErrors)}
        placeholder={i18n.calendar.reservationModal.start}
        data-qa={dataQa ? `${dataQa}-start` : undefined}
      />
      <span>–</span>
      <TimeInput
        value={endTime}
        onChange={onChangeEnd}
        info={errorToInputInfo(errors.endTime, i18n.validationErrors)}
        placeholder={i18n.calendar.reservationModal.end}
        data-qa={dataQa ? `${dataQa}-end` : undefined}
      />
    </>
  )
})
