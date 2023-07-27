// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Main from 'lib-components/atoms/Main'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { useTranslation } from '../localization'

import PersonalDetailsSection from './PersonalDetailsSection'

export default React.memo(function PersonalDetails() {
  const t = useTranslation()
  return (
    <Main>
      <Container>
        <Gap size="L" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.personalDetails.title}</H1>
          {t.personalDetails.description}
          <PersonalDetailsSection />
        </ContentArea>
      </Container>
      <Footer />
    </Main>
  )
})
