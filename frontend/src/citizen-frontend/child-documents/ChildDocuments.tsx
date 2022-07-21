// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Footer from 'citizen-frontend/Footer'
import Main from 'lib-components/atoms/Main'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../localization'

import CitizenVasuAndLeops from './CitizenVasuAndLeops'
import PedagogicalDocuments from './PedagogicalDocuments'

export default React.memo(function ChildDocuments() {
  const t = useTranslation()
  return (
    <>
      <Main>
        <Container>
          <Gap size="s" />

          <ContentArea opaque paddingVertical="L">
            <H1 noMargin>{t.childDocuments.title}</H1>
            <P>{t.childDocuments.description}</P>
            <P>{t.childDocuments.deletionDisclaimer}</P>
          </ContentArea>

          <Gap size="s" />

          {featureFlags.experimental?.citizenVasu && (
            <>
              <CitizenVasuAndLeops />

              <Gap size="s" />
            </>
          )}

          <PedagogicalDocuments />
        </Container>
      </Main>

      <Footer />
    </>
  )
})
