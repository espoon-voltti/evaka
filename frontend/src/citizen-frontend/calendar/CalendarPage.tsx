// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import Container, { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import CalendarListView from './CalendarListView'
import { DailyReservationData, getReservations } from './api'
import LocalDate from 'lib-common/local-date'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import { useTranslation } from '../localization'
import { useUser } from '../auth'

export default React.memo(function CalendarPage() {
  const i18n = useTranslation()
  const user = useUser()

  const [data, setData] = useState<Result<DailyReservationData[]>>(Loading.of())

  const loadData = useRestApi(getReservations, setData)
  useEffect(
    () =>
      loadData(
        LocalDate.today().startOfWeek(),
        LocalDate.today().addMonths(2).startOfWeek().subDays(1)
      ),
    [loadData]
  )

  if (!user || !user.accessibleFeatures.reservations) return null

  return (
    <>
      <Container>
        <ContentArea opaque paddingVertical="zero" paddingHorizontal="zero">
          {data.mapAll({
            loading() {
              return <Loader />
            },
            failure() {
              return <div>{i18n.common.errors.genericGetError}</div>
            },
            success(dallyReservations) {
              return <CalendarListView dailyReservations={dallyReservations} />
            }
          })}
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
})
