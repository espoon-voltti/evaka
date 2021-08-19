// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Container, { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import CalendarListView from './CalendarListView'
import {getReservations} from "./api";
import LocalDate from "../../lib-common/local-date";

export default React.memo(function CalendarPage() {
  void getReservations(
    LocalDate.today().startOfWeek(),
    LocalDate.today().addMonths(2).startOfWeek()
  )

  return (
    <>
      <Container>
        <ContentArea opaque paddingVertical="zero" paddingHorizontal="zero">
          <CalendarListView />
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
})
