// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import Container, { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import CalendarListView from './CalendarListView'
import { getReservations, ReservationsResponse } from './api'
import LocalDate from 'lib-common/local-date'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import { useTranslation } from '../localization'
import { useUser } from '../auth'
import ReservationModal from './ReservationModal'

export default React.memo(function CalendarPage() {
  const i18n = useTranslation()
  const user = useUser()

  const [data, setData] = useState<Result<ReservationsResponse>>(Loading.of())
  const [reservationViewOpen, setReservationViewOpen] = useState(false)

  const loadData = useRestApi(getReservations, setData)
  const loadDefaultRange = useCallback(
    () =>
      loadData(
        LocalDate.today().startOfWeek(),
        LocalDate.today().addMonths(2).startOfWeek().subDays(1)
      ),
    [loadData]
  )

  useEffect(loadDefaultRange, [loadDefaultRange])

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
            success(response) {
              return (
                <>
                  <CalendarListView
                    dailyReservations={response.dailyData}
                    onCreateReservationClicked={() =>
                      setReservationViewOpen(true)
                    }
                  />
                  {reservationViewOpen && (
                    <ReservationModal
                      onClose={() => setReservationViewOpen(false)}
                      availableChildren={response.children}
                      onReload={loadDefaultRange}
                    />
                  )}
                </>
              )
            }
          })}
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
})
