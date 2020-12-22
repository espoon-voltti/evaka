{/*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/}

import * as React from 'react'
import { withOptions } from '@storybook/addon-options'
import { configure, addDecorator } from '@storybook/react'
import { Container, ContentArea } from '@evaka/lib-components/src/layout/Container'
// automatically import all files ending in *.stories.tsx
const req = require.context('../src', true, /.stories.tsx$/)

function loadStories() {
  req.keys().forEach(req)
}

const storyWrapper = (story) => (
  <Container>
    <ContentArea opaque>{story()}</ContentArea>
  </Container>
)

addDecorator(
  withOptions({
    goFullScreen: false,
    showStoriesPanel: true,
    showAddonPanel: true,
    showSearchBox: false,
    addonPanelInRight: true,
    sortStoriesByKind: true
  })
)

addDecorator(storyWrapper)

configure(loadStories, module)
