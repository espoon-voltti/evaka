import React from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Button } from 'lib-components/atoms/buttons/Button'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

export default React.memo(function OutOfOfficePage() {
  const { i18n } = useTranslation()

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.outOfOffice.title}</H1>
        <P>{i18n.outOfOffice.description}</P>
        <Gap size="m" />
        <Label>{i18n.outOfOffice.header}</Label>
        <P>{i18n.outOfOffice.noFutureOutOfOffice}</P>
        <Button text={i18n.outOfOffice.addOutOfOffice} primary />
      </ContentArea>
    </Container>
  )
})
