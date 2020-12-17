// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'

// todo: write new container components
import { Container, ContentArea } from 'components/shared/layout/Container'
import { useTranslation } from '~state/i18n'

interface PageWrapperProps {
  children: React.ReactNode
  withReturn?: boolean
}

export default React.memo(function PageWrapper({
  children,
  withReturn
}: PageWrapperProps) {
  const { i18n } = useTranslation()
  return (
    <Container>
      {withReturn && <ReturnButton label={i18n.common.goBack} />}
      <ContentArea opaque>{children}</ContentArea>
    </Container>
  )
})
