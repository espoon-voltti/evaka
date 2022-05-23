// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../localization'

import CitizenVasuAndLeops from './CitizenVasuAndLeops'
import PedagogicalDocuments from './PedagogicalDocuments'

export default React.memo(function ChildDocuments() {
  const t = useTranslation()
  return (
    <>
      <Container>
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.childDocuments.title}</H1>
          <p>{t.childDocuments.description}</p>
        </ContentArea>

        <Gap size="s" />

        <CitizenVasuAndLeops />

        <Gap size="s" />

        <PedagogicalDocuments />
      </Container>
    </>
  )
})
