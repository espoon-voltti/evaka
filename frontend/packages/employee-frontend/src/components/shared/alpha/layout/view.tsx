// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { ContentArea } from './content-area'
import { Container } from './container'
import { ContentHeader } from './content-header'

interface Props {
  title: string
  dataQa: string
  subtitle: string
  status: React.ReactNode
  children: React.ReactNode
  breadcrumbs?: React.ReactNode
  opaque?: boolean
}

export const View = ({
  children,
  title,
  dataQa,
  subtitle,
  status,
  breadcrumbs,
  opaque = true
}: Props) => (
  <div className="view" data-qa={dataQa}>
    <Container>
      {breadcrumbs}
      <ContentArea opaque={opaque}>
        <ContentHeader title={title} subtitle={subtitle} status={status} />
        {children}
      </ContentArea>
    </Container>
  </div>
)
