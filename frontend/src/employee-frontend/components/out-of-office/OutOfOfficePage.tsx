// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Button } from 'lib-components/atoms/buttons/Button'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import OutOfOfficeEditor from './OutOfOfficeEditor'

export default React.memo(function OutOfOfficePage() {
  const { i18n } = useTranslation()
  const [isEditing, setIsEditing] = useState(false)

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.outOfOffice.title}</H1>
        <P>{i18n.outOfOffice.description}</P>
        <Gap size="m" />
        <Label>{i18n.outOfOffice.header}</Label>
        <Gap size="s" />
        {isEditing ? (
          <OutOfOfficeEditor onClose={() => setIsEditing(false)} />
        ) : (
          <Fragment>
            <div>{i18n.outOfOffice.noFutureOutOfOffice}</div>
            <Gap size="m" />
            <Button
              text={i18n.outOfOffice.addOutOfOffice}
              primary
              onClick={() => setIsEditing(true)}
            />
          </Fragment>
        )}
      </ContentArea>
    </Container>
  )
})
