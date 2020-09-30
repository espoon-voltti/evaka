// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { storiesOf } from '@storybook/react'
import { FixedSpaceColumn } from 'components/shared/layout/flex-helpers'
import Radio from 'components/shared/atoms/form/Radio'

function StatefulStory({ disabled = false }: { disabled?: boolean }) {
  const [selection, setSelection] = useState(1)

  return (
    <FixedSpaceColumn>
      <Radio
        checked={selection === 1}
        onChange={() => setSelection(1)}
        disabled={disabled}
        name="foo"
        label="Vaihtoehto 1"
      />
      <Radio
        checked={selection === 2}
        onChange={() => setSelection(2)}
        disabled={disabled}
        name="foo"
        label="Vaihtoehto 2"
      />
      <Radio
        checked={selection === 3}
        onChange={() => setSelection(3)}
        disabled={disabled}
        name="foo"
        label="Vaihtoehto 3"
      />
    </FixedSpaceColumn>
  )
}

storiesOf('evaka/atoms/form/Radio', module)
  .add('default', () => <StatefulStory />)
  .add('disabled', () => <StatefulStory disabled />)
