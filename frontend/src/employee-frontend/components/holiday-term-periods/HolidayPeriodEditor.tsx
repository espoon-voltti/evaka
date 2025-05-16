// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import { useNavigate } from 'react-router'

import type { HolidayPeriodId } from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import { constantQuery, useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Container, { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'

import HolidayPeriodForm from './HolidayPeriodForm'
import { holidayPeriodQuery } from './queries'

export default React.memo(function HolidayPeriodEditor() {
  const { id } = useRouteParams(['id'])
  const holidayPeriodId =
    id === 'new' ? undefined : fromUuid<HolidayPeriodId>(id)

  const holidayPeriod = useQueryResult(
    holidayPeriodId
      ? holidayPeriodQuery({ id: holidayPeriodId })
      : constantQuery(null)
  )

  const navigate = useNavigate()

  const navigateToList = useCallback(
    () => void navigate('/holiday-periods'),
    [navigate]
  )

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(holidayPeriod, (holiday) =>
          holiday === null ? (
            <HolidayPeriodForm
              onSuccess={navigateToList}
              onCancel={navigateToList}
            />
          ) : (
            <HolidayPeriodForm
              holidayPeriod={holiday}
              onSuccess={navigateToList}
              onCancel={navigateToList}
            />
          )
        )}
      </ContentArea>
    </Container>
  )
})
