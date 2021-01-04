// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Decision } from '~decisions/types'
import { client } from '~api-client'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { H1 } from '@evaka/lib-components/src/typography'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import styled from 'styled-components'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'

const getDecisions = async (): Promise<Decision[]> => {
  const { data } = await client.get<Decision[]>('/citizen/decisions')
  return data
}

export default React.memo(function Decisions() {
  const [decisions, setDecisions] = useState<Decision[]>([])

  useEffect(() => {
    void getDecisions().then(setDecisions)
  }, [])

  return (
    <Container>
      <FixedSpaceColumn>
        <ContentArea opaque>
          <H1>Päätökset</H1>
          <Paragraph>
            Tälle sivulle saapuvat lapsen varhaiskasvatus-, esiopetus- ja
            kerhohakemuksiin liittyvät päätökset. Uuden päätöksen saapuessa
            sinun tulee kahden viikon sisällä vastata, hyväksytkö vai hylkäätkö
            lapselle tarjotun paikan.
          </Paragraph>
          {decisions.length > 0 ? (
            <AlertBox
              message={`${decisions.length} päätöstä odottaa vahvistusta`}
              thin
            />
          ) : null}
        </ContentArea>

        {decisions.map((decision) => (
          <ContentArea opaque key={decision.id}>
            {decision.id}
          </ContentArea>
        ))}
      </FixedSpaceColumn>
    </Container>
  )
})

const Paragraph = styled.p`
  width: 800px;
`
