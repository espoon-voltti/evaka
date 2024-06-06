// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Button } from '../atoms/buttons/Button'
import Container, { ContentArea } from '../layout/Container'
import { H1, P } from '../typography'

const ErrorContainer = styled(ContentArea)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
`

interface Labels {
  title: string
  text: string
  reload: string
}

interface Props {
  basePath: string
  labels: Labels
}

export default React.memo(function ErrorPage({
  basePath,
  labels: { reload, text, title }
}: Props) {
  const onClick = () => window.location.replace(basePath)
  return (
    <main>
      <Container>
        <ErrorContainer opaque paddingVertical="XL">
          <H1>{title}</H1>
          <P centered>{text}</P>
          <Button primary onClick={onClick}>
            {reload}
          </Button>
        </ErrorContainer>
      </Container>
    </main>
  )
})
