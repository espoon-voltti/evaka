// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { RouteComponentProps } from 'react-router'
import { useHistory } from 'react-router-dom'
import { Success } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { renderResult } from '../async-rendering'
import HolidayPeriodForm from './HolidayPeriodForm'
import { getHolidayPeriod } from './api'

export default React.memo(function HolidayPeriodEditor({
  match
}: RouteComponentProps<{ id: string }>) {
  const holidayPeriodId =
    match.params.id === 'new' ? undefined : match.params.id

  const [holidayPeriod] = useApiState(
    () =>
      holidayPeriodId
        ? getHolidayPeriod(holidayPeriodId)
        : Promise.resolve(Success.of(undefined)),
    [holidayPeriodId]
  )

  const history = useHistory()

  const navigateToList = useCallback(
    () => history.push('/holiday-periods'),
    [history]
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
