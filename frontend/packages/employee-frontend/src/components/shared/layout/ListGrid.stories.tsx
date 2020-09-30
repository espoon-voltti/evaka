// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Label } from 'components/shared/Typography'
import { storiesOf } from '@storybook/react'
import ListGrid from 'components/shared/layout/ListGrid'

storiesOf('evaka/layout/ListGrid', module).add('default', () => (
  <ListGrid>
    <Label>Nimi</Label>
    <span>Aku Ankka</span>

    <Label>Syntymävuosi</Label>
    <span>1952</span>

    <Label>Huollettavat</Label>
    <span>Tupu Ankka</span>
    <Label />
    <span>Hupu Ankka</span>
    <Label />
    <span>Lupu Ankka</span>

    <Label>Elämäntarina syntymästä nykyisyyteen</Label>
    <p>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
      veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
      commodo consequat. Duis aute irure dolor in reprehenderit in voluptate
      velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
      cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
      est laborum.
    </p>
  </ListGrid>
))
