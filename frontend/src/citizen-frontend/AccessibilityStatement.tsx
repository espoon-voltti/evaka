// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Main from 'lib-components/atoms/Main'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'

import Footer from './Footer'
import { useTranslation } from './localization'
import useTitle from './useTitle'

export default React.memo(function AccessibilityStatement() {
  const t = useTranslation()
  useTitle(t, t.footer.accessibilityStatement)

  return (
    <>
      <Main>
        <Container>
          <ReturnButton label={t.common.return} />
          <ContentArea opaque data-qa="accessibility-statement">
            {t.accessibilityStatement}
          </ContentArea>
        </Container>
      </Main>
      <Footer />
    </>
  )
})
