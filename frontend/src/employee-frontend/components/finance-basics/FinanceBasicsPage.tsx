{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'
import { H1 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { useTranslation } from '../../state/i18n'
import FeesSection from './FeesSection'

export default React.memo(function FinanceBasicsPage() {
  const { i18n } = useTranslation()

  return (
    <Container verticalMargin={defaultMargins.L} data-qa="finance-basics-page">
      <FixedSpaceColumn fullWidth>
        <ContentArea opaque>
          <H1 noMargin>{i18n.financeBasics.title}</H1>
        </ContentArea>
        <FeesSection />
      </FixedSpaceColumn>
    </Container>
  )
})
