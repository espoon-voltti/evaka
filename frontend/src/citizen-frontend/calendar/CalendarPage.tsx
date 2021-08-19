// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Container, { ContentArea } from 'lib-components/layout/Container'
import Footer from '../Footer'
import CalendarListView from './CalendarListView'

export default React.memo(function CalendarPage() {
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
