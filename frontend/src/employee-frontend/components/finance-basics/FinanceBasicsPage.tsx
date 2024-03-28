// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H1 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

import FeesSection from './FeesSection'
import ServiceNeedsSection from './ServiceNeedsSection'

export default React.memo(function FinanceBasicsPage() {
  const { i18n } = useTranslation()

  return (
    <Container data-qa="finance-basics-page">
      <FixedSpaceColumn fullWidth>
        <ContentArea opaque>
          <H1 noMargin>{i18n.titles.financeBasics}</H1>
        </ContentArea>
        <FeesSection />
        <ServiceNeedsSection />
      </FixedSpaceColumn>
    </Container>
  )
})
