// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { storiesOf } from '@storybook/react'
import ListGrid from 'components/shared/layout/ListGrid'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { Label } from '@evaka/lib-components/src/typography'

function StatefulStory() {
  const [text1, setText1] = useState('')
  const [text2, setText2] = useState('')
  const [text3, setText3] = useState('')
  const [text4, setText4] = useState('')
  const [text5, setText5] = useState('')
  const [text6, setText6] = useState('1,75')
  const [text7, setText7] = useState('+358 50 123 4567')
  const [text8, setText8] = useState(
    'Ankkalinnantie 313 A 42, 00480 Ankkalinna'
  )
  const [text9, setText9] = useState('')

  return (
    <ListGrid rowGap="L">
      <Label inputRow>Tekstikenttä</Label>
      <InputField value={text1} onChange={setText1} />
      <Label inputRow>Placeholder</Label>
      <InputField
        value={text2}
        onChange={setText2}
        placeholder={'Tämä on placeholder'}
      />
      <Label inputRow>Info</Label>
      <InputField
        value={text3}
        onChange={setText3}
        info={{ text: 'Syötä päivämäärä muodossa pp.kk.vvvv' }}
      />
      <Label inputRow>Info success</Label>
      <InputField
        value={text4}
        onChange={setText4}
        info={
          text4.length > 0
            ? { text: 'Hyvältä näyttää!', status: 'success' }
            : undefined
        }
      />
      <Label inputRow>Info warning</Label>
      <InputField
        value={text5}
        onChange={setText5}
        info={
          text5.length > 0
            ? { text: 'Virheellinen arvo', status: 'warning' }
            : undefined
        }
      />
      <Label inputRow>Read only</Label>
      <InputField value="Tätä ei voi muokata" readonly />
      <Label inputRow>Width s</Label>
      <InputField value={text6} onChange={setText6} width="s" />
      <Label inputRow>Width m</Label>
      <InputField value={text7} onChange={setText7} width="m" />
      <Label inputRow>Width L</Label>
      <InputField value={text8} onChange={setText8} width="L" />
      <Label inputRow>Clearable</Label>
      <InputField value={text9} onChange={setText9} clearable />
    </ListGrid>
  )
}

storiesOf('evaka/atoms/form/InputField', module).add('all', () => (
  <StatefulStory />
))
