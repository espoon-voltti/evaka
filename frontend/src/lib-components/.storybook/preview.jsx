{/*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/}

import * as React from 'react'
import { addDecorator, addParameters } from '@storybook/react'
import { Container, ContentArea } from '@evaka/lib-components/layout/Container'

const storyWrapper = (story) => (
  <Container>
    <ContentArea opaque>{story()}</ContentArea>
  </Container>
)

addParameters({
  options: {
    goFullScreen: false,
    showStoriesPanel: true,
    showAddonPanel: true,
    showSearchBox: false,
    addonPanelInRight: true,
    sortStoriesByKind: true
  }
})

addDecorator(storyWrapper)
