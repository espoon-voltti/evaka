// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import ErrorSegment from './ErrorSegment'

storiesOf('evaka/atoms/state/ErrorSegment', module)
  .add('default', () => <ErrorSegment />)
  .add('With info', () => (
    <ErrorSegment info="Käy keittämässä kahvit ja yritä uudelleen" />
  ))
  .add('Compact', () => <ErrorSegment compact />)
