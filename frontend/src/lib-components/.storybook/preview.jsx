{/*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/}

import * as React from 'react'
import { addDecorator, addParameters } from '@storybook/react'
import { Container, ContentArea } from 'lib-components/layout/Container'

import '../../citizen-frontend/index.css'
import { ThemeProvider } from 'styled-components'
import theme from 'lib-common/themes/espoo-theme'

const storyWrapper = (story) => (
  <ThemeProvider theme={theme}>
    <Container>
        <ContentArea opaque>{story()}</ContentArea>
    </Container>
  </ThemeProvider>
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
