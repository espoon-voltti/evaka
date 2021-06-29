// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { storiesOf } from '@storybook/react'
import { Label } from '../../typography'
import TextArea from './TextArea'
import { Gap } from 'lib-components/white-space'

function StatefulStory() {
  const [text1, setText1] = useState('')
  const [text2, setText2] = useState('2')
  const [text3, setText3] = useState('Kolme\nriviä\ntekstiä.')
  const [text4, setText4] = useState('4')
  const [text5, setText5] = useState('')

  return (
    <>
      <Label inputRow>Tekstialue</Label>
      <TextArea value={text1} onChange={() => setText1} />
      <Gap size="L" />
      <Label inputRow>Placeholder</Label>
      <TextArea
        value={text2}
        onChange={() => setText2}
        placeholder={'Tämä on placeholder'}
      />
      <Gap size="L" />
      <Label inputRow>Info</Label>
      <TextArea
        value={text3}
        onChange={() => setText3}
        info={{ text: 'Tiedon syöttämisen ohjeistus' }}
      />
      <Gap size="L" />
      <Label inputRow>Info success</Label>
      <TextArea
        value={text4}
        onChange={() => setText4}
        info={
          text4.length > 0
            ? { text: 'Hyvältä näyttää!', status: 'success' }
            : undefined
        }
      />
      <Gap size="L" />
      <Label inputRow>Info warning</Label>
      <TextArea
        value={text5}
        onChange={() => setText5}
        info={
          text5.length > 0
            ? { text: 'Pakollinen tieto!', status: 'warning' }
            : undefined
        }
      />
      <Gap size="L" />
      <Label inputRow>Read only</Label>
      <TextArea value="Tätä ei voi muokata" readonly />
    </>
  )
}

storiesOf('evaka/atoms/form/TextArea', module).add('all', () => (
  <StatefulStory />
))
