// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { action } from '@storybook/addon-actions'
import { storiesOf } from '@storybook/react'
import { faChevronLeft, faPen, faTrash } from 'lib-icons'
import React from 'react'
import { H3 } from '../../typography'
import { Gap } from '../../white-space'
import AddButton from './AddButton'
import AsyncButton from './AsyncButton'
import Button from './Button'
import IconButton from './IconButton'
import InlineButton from './InlineButton'

const onClick = action('clicked')

storiesOf('evaka/atoms/buttons', module)
  .add('Button', () => (
    <div>
      <H3>Primary</H3>
      <Button onClick={onClick} text="Vahvista" primary />
      <Gap />

      <H3>Default</H3>
      <Button onClick={onClick} text="Peruuta" />
      <Gap />

      <H3>Primary, disabled</H3>
      <Button onClick={onClick} text="Vahvista" primary disabled />
      <Gap />

      <H3>Default, disabled</H3>
      <Button onClick={onClick} text="Peruuta" disabled />
    </div>
  ))
  .add('InlineButton', () => (
    <div>
      <H3>Default</H3>
      <InlineButton onClick={onClick} text="Muokkaa" />
      <Gap />

      <H3>Default, disabled</H3>
      <InlineButton onClick={onClick} text="Poista" disabled />
      <Gap />

      <H3>With icon</H3>
      <InlineButton onClick={onClick} text="Muokkaa" icon={faPen} />
      <Gap />

      <H3>With icon, disabled</H3>
      <InlineButton onClick={onClick} text="Poista" icon={faTrash} disabled />
      <Gap />

      <H3>Darker</H3>
      <p>Darker inline button is used on grey background.</p>
      <InlineButton onClick={onClick} text="Palaa" darker />
      <Gap />

      <H3>Darker, disabled</H3>
      <InlineButton onClick={onClick} text="Palaa" darker disabled />
      <Gap />

      <H3>Darker, with icon</H3>
      <InlineButton
        onClick={onClick}
        text="Palaa"
        icon={faChevronLeft}
        darker
      />
      <Gap />

      <H3>Darker, with icon, disabled</H3>
      <InlineButton
        onClick={onClick}
        text="Palaa"
        icon={faChevronLeft}
        darker
        disabled
      />
      <Gap />
    </div>
  ))
  .add('IconButton', () => (
    <div>
      <H3>Default</H3>
      <IconButton onClick={onClick} altText="Muokkaa" icon={faPen} />
      <Gap />

      <H3>Disabled</H3>
      <IconButton onClick={onClick} altText="Poista" icon={faTrash} disabled />
      <Gap />
    </div>
  ))
  .add('AddButton', () => (
    <div>
      <H3>Default</H3>
      <AddButton onClick={onClick} text="Luo uusi sijoitus" />
      <Gap />

      <H3>Disabled</H3>
      <AddButton onClick={onClick} text="Luo uusi sijoitus" disabled />
      <Gap />

      <H3>Flipped</H3>
      <AddButton onClick={onClick} text="Luo uusi sijoitus" flipped />
      <Gap />

      <H3>Darker</H3>
      <AddButton onClick={onClick} text="Luo uusi sijoitus" darker />

      <H3>Darker, disabled</H3>
      <AddButton onClick={onClick} text="Luo uusi sijoitus" darker disabled />
      <Gap />
    </div>
  ))
  .add('AsyncButton', () => (
    <div>
      <H3>Default (Success)</H3>
      <AsyncButton
        onClick={() =>
          new Promise<void>((resolve) => void setTimeout(resolve, 2000))
        }
        onSuccess={() => console.log('success')}
        text="Save"
        textInProgress="Saving"
        textDone="Saved"
      />
      <Gap />

      <H3>Default (Failure)</H3>
      <AsyncButton
        onClick={() =>
          new Promise<void>((resolve) => void setTimeout(resolve, 2000)).then(
            () => {
              throw 'Error'
            }
          )
        }
        onSuccess={() => undefined}
        text="Save"
        textInProgress="Saving"
        textDone="Saved"
      />
      <Gap />

      <H3>Primary</H3>
      <AsyncButton
        onClick={() =>
          new Promise<void>((resolve) => void setTimeout(resolve, 2000))
        }
        onSuccess={() => undefined}
        text="Save"
        textInProgress="Saving"
        textDone="Saved"
        primary
      />
      <Gap />

      <H3>Disabled</H3>
      <AsyncButton
        onClick={() => new Promise(() => undefined)}
        onSuccess={() => undefined}
        text="Save"
        textInProgress="Saving"
        textDone="Saved"
        disabled
      />
      <Gap />
    </div>
  ))
