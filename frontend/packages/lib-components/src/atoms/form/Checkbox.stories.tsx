// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { storiesOf } from '@storybook/react'
import { FixedSpaceColumn } from '../../layout/flex-helpers'
import Checkbox from './Checkbox'

function StatefulStory() {
  const [c1, setC1] = useState(true)
  const [c2, setC2] = useState(false)

  return (
    <FixedSpaceColumn>
      <Checkbox
        checked={c1}
        onChange={(val) => setC1(val)}
        label="Kissat on kivoja"
      />
      <Checkbox
        checked={c2}
        onChange={(val) => setC2(val)}
        label="Marsut on kivoja"
      />
      <Checkbox checked={true} disabled label="Koirat on kivoja" />
      <Checkbox checked={false} disabled label="Hämähäkit on kivoja" />
    </FixedSpaceColumn>
  )
}

storiesOf('evaka/atoms/form/Checkbox', module).add('default', () => (
  <StatefulStory />
))
