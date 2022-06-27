// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'

import Main from 'lib-components/atoms/Main'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'

import Footer from './Footer'
import { useTranslation } from './localization'

export interface Props {
  scrollToTop: () => void
}

export default React.memo(function AccessibilityStatement({
  scrollToTop
}: Props) {
  const t = useTranslation()

  // Scroll the main content area to top on first mount
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => scrollToTop(), [])

  return (
    <>
      <Main>
        <Container>
          <ReturnButton label={t.common.return} />
          <ContentArea opaque>{t.accessibilityStatement}</ContentArea>
        </Container>
      </Main>
      <Footer />
    </>
  )
})
