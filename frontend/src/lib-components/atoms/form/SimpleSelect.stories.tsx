// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { storiesOf } from '@storybook/react'
import { FixedSpaceColumn } from '../../layout/flex-helpers'
import SimpleSelect from './SimpleSelect'

storiesOf('evaka/atoms/form/SimpleSelect', module).add('default', () => {
  const options = [
    {
      value: 'red',
      label: 'Red'
    },
    {
      value: 'blue',
      label: 'Blue'
    },
    {
      value: 'green',
      label: 'Green'
    },
    {
      value: 'yellow',
      label: 'Yellow'
    }
  ]

  function StatefulStory() {
    const [value, setValue] = useState<string>()

    return (
      <FixedSpaceColumn>
        <SimpleSelect
          value={value}
          options={options}
          onChange={(e) =>
            setValue(e.target.value ? e.target.value : undefined)
          }
          placeholder={' '}
        />
      </FixedSpaceColumn>
    )
  }

  return <StatefulStory />
})
