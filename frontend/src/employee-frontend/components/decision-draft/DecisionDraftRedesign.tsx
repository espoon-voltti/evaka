// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'

import { useTranslation } from '../../state/i18n'

export default React.memo(function DecisionDraftRedesign() {
  const { i18n } = useTranslation()
  return (
    <Container>
      <ContentArea $opaque>
        <Title size={1}>{i18n.decisionDraft.title}</Title>
        <p>Decision draft redesign — work in progress.</p>
      </ContentArea>
    </Container>
  )
})
