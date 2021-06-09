// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import Title from 'lib-components/atoms/Title'
import { P } from 'lib-components/typography'

import { useTranslation } from 'employee-frontend/state/i18n'

export default React.memo(function WelcomePage() {
  const { i18n } = useTranslation()

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <Title>{i18n.titles.welcomePage}</Title>
        <P>{i18n.welcomePage.text}</P>
      </ContentArea>
    </Container>
  )
})
