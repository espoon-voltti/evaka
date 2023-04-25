// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

export default React.memo(function VasuTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.documentTemplates

  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.title}</H1>
      </ContentArea>
    </Container>
  )
})
