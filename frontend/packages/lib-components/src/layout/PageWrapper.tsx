// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ReturnButton from '../atoms/buttons/ReturnButton'
import { Container, ContentArea } from './Container'

type Props = {
  children: React.ReactNode
}

type WithoutReturnProps = {
  withReturn: undefined
  history: undefined
  goBackLabel: undefined
}

type WithReturnProps = {
  withReturn: true
  goBackLabel: string
}

export default React.memo(function PageWrapper({
  children,
  ...props
}: (Props & WithoutReturnProps) | (Props & WithReturnProps)) {
  return (
    <Container>
      {props.withReturn && <ReturnButton label={props.goBackLabel} />}
      <ContentArea opaque>{children}</ContentArea>
    </Container>
  )
})
