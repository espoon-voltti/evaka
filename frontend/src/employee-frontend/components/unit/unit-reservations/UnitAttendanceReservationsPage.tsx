// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Loading, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import {
  getUnitAttendanceReservations,
  UnitAttendanceReservations
} from 'employee-frontend/api/unit'
import { useTranslation } from 'employee-frontend/state/i18n'
import ReservationsTable from './ReservationsTable'

export default React.memo(function UnitAttendanceReservationsPage() {
  const { unitId } = useParams<{ unitId: string }>()
  const { i18n } = useTranslation()

  const [reservations, setReservations] = useState<
    Result<UnitAttendanceReservations>
  >(Loading.of())
  const loadAttendanceReservations = useRestApi(
    getUnitAttendanceReservations,
    setReservations
  )

  useEffect(
    () => loadAttendanceReservations(unitId, LocalDate.today()),
    [unitId, loadAttendanceReservations]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {reservations.mapAll({
          loading() {
            return <Loader />
          },
          failure() {
            return <div>{i18n.common.error.unknown}</div>
          },
          success(data) {
            return (
              <>
                <H2>{data.unit}</H2>
                <Gap size="m" />
                <FixedSpaceColumn spacing="L">
                  {data.groups.map(({ group, children }) => (
                    <div key={group}>
                      <H3 noMargin>{group}</H3>
                      <ReservationsTable
                        operationalDays={data.operationalDays}
                        reservations={children}
                      />
                    </div>
                  ))}
                  {data.ungrouped.length > 0 ? (
                    <div>
                      <H3 noMargin>
                        {i18n.unit.attendanceReservations.ungrouped}
                      </H3>
                      <ReservationsTable
                        operationalDays={data.operationalDays}
                        reservations={data.ungrouped}
                      />
                    </div>
                  ) : null}
                </FixedSpaceColumn>
              </>
            )
          }
        })}
      </ContentArea>
      <Gap size="L" />
    </Container>
  )
})
