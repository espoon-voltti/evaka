// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { storiesOf } from '@storybook/react'
import React from 'react'
import ErrorSegment from 'components/shared/atoms/state/ErrorSegment'

storiesOf('evaka/atoms/state/ErrorSegment', module)
  .add('default', () => <ErrorSegment />)
  .add('With info', () => (
    <ErrorSegment info="Käy keittämässä kahvit ja yritä uudelleen" />
  ))
  .add('Compact', () => <ErrorSegment compact />)
