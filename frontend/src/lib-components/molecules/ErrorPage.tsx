// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import React from 'react'
import Button from '../atoms/buttons/Button'
import Container, { ContentArea } from '../layout/Container'
import { H1, P } from '../typography'

const ErrorContainer = styled(ContentArea)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
`

function getApplicationBasePath() {
  const path = window.location.pathname
  if (path.startsWith('/employee/mobile')) {
    return '/employee/mobile'
  } else if (path.startsWith('/employee')) {
    return '/employee'
  }
  return '/'
}

interface Labels {
  title: string
  text: string
  reload: string
}

interface Props {
  labels: Labels
}

export function ErrorPage({ labels: { reload, text, title } }: Props) {
  const onClick = () => {
    window.location.replace(getApplicationBasePath())
  }
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
}
