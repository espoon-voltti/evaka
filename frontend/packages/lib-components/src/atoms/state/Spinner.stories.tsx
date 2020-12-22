// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import Spinner, { SpinnerSegment } from './Spinner'

storiesOf('evaka/atoms/state/spinner', module)
  .add('default', () => <Spinner />)
  .add('segment', () => <SpinnerSegment />)
