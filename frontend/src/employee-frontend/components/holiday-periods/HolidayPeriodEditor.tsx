// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { Success } from 'lib-common/api'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import HolidayPeriodForm from './HolidayPeriodForm'
import { getHolidayPeriod } from './api'

export default React.memo(function HolidayPeriodEditor() {
  const { id } = useNonNullableParams<{ id: string }>()
  const holidayPeriodId = id === 'new' ? undefined : id

  const [holidayPeriod] = useApiState(
    () =>
      holidayPeriodId
        ? getHolidayPeriod(holidayPeriodId)
        : Promise.resolve(Success.of(undefined)),
    [holidayPeriodId]
  )

  const navigate = useNavigate()

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(holidayPeriod, (holiday) => (
          <HolidayPeriodForm
            holidayPeriod={holiday}
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        ))}
      </ContentArea>
    </Container>
  )
})
