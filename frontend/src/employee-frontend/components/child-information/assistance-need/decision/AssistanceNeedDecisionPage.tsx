// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Container from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'

import { FooterContainer } from './common'

export default React.memo(function AssistanceNeedDecisionPage() {
  const { id } = useNonNullableParams<{ id: UUID }>()

  return (
    <Container>
      preview {id}
      <StickyFooter>
        <FooterContainer></FooterContainer>
      </StickyFooter>
    </Container>
  )
})
