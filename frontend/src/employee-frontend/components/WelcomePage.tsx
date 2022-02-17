// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { P } from 'lib-components/typography'

import { useTranslation } from '../state/i18n'

export default React.memo(function WelcomePage() {
  const { i18n } = useTranslation()

  return (
    <Container>
      <ContentArea opaque>
        <Title>{i18n.titles.welcomePage}</Title>
        <P>{i18n.welcomePage.text}</P>
      </ContentArea>
    </Container>
  )
})
