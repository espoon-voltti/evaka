// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Container, ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'

export default React.memo(function TodoItemsPage() {
  return (
    <Container>
      <ContentArea $opaque>
        <H1>Tehtävälista</H1>
      </ContentArea>
    </Container>
  )
})
