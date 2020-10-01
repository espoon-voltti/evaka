// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'

// todo: write new container components
import { Container, ContentArea } from 'components/shared/layout/Container'

interface PageWrapperProps {
  children: React.ReactNode
  withReturn?: boolean
}

function PageWrapper({ children, withReturn }: PageWrapperProps) {
  return (
    <Container>
      {withReturn && <ReturnButton />}
      <ContentArea opaque>{children}</ContentArea>
    </Container>
  )
}

export default PageWrapper
