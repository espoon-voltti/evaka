// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import HolidayPeriodForm from './HolidayPeriodForm'
import { holidayPeriodQuery } from './queries'

export default React.memo(function HolidayPeriodEditor() {
  const { id } = useNonNullableParams<{ id: string }>()

  const holidayPeriod = useQueryResult(holidayPeriodQuery({ id }), {
    enabled: id !== 'new'
  })

  const navigate = useNavigate()

  const navigateToList = useCallback(
    () => navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {id === 'new' ? (
          <HolidayPeriodForm
            onSuccess={navigateToList}
            onCancel={navigateToList}
          />
        ) : (
          renderResult(holidayPeriod, (holiday) => (
            <HolidayPeriodForm
              holidayPeriod={holiday}
              onSuccess={navigateToList}
              onCancel={navigateToList}
            />
          ))
        )}
      </ContentArea>
    </Container>
  )
})
