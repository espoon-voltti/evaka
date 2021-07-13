// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { storiesOf } from '@storybook/react'
import React, { useState } from 'react'
import { FixedSpaceColumn } from '../../layout/flex-helpers'
import Combobox from './Combobox'

interface Option {
  id: string
  name: string
}

storiesOf('evaka/atoms/form/Combobox', module).add('default', () => {
  const options: Option[] = [
    {
      id: 'dog',
      name: 'Dog 🐕'
    },
    {
      id: 'cat',
      name: 'Cat 🐈'
    },
    {
      id: 'bird',
      name: 'Bird 🦜'
    },
    {
      id: 'snake',
      name: 'Snake 🐍'
    }
  ]

  function StatefulStory() {
    const [value, setValue] = useState<Option | null>(null)

    return (
      <FixedSpaceColumn>
        <Combobox
          selectedItem={value}
          items={options}
          getItemLabel={({ name }) => name}
          onChange={(e) => setValue(e ?? null)}
          placeholder={'Pick an animal'}
          clearable
        />
      </FixedSpaceColumn>
    )
  }

  return <StatefulStory />
})
