// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import { H3 } from '@evaka/lib-components/src/typography'
import Button from 'components/shared/atoms/buttons/Button'
import { action } from '@storybook/addon-actions'
import { Gap } from '@evaka/lib-components/src/white-space'
import InlineButton from 'components/shared/atoms/buttons/InlineButton'
import { faPen, faTrash } from '@evaka/lib-icons'
import IconButton from 'components/shared/atoms/buttons/IconButton'
import AddButton from 'components/shared/atoms/buttons/AddButton'
import AsyncButton from 'components/shared/atoms/buttons/AsyncButton'

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

      <H3>Disabled</H3>
      <InlineButton onClick={onClick} text="Poista" disabled />
      <Gap />

      <H3>With icon</H3>
      <InlineButton onClick={onClick} text="Muokkaa" icon={faPen} />
      <Gap />

      <H3>With icon, disabled</H3>
      <InlineButton onClick={onClick} text="Poista" icon={faTrash} disabled />
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
    </div>
  ))
  .add('AsyncButton', () => {
    return (
      <div>
        <H3>Default (Success)</H3>
        <AsyncButton
          onClick={() =>
            new Promise<void>((resolve) => void setTimeout(resolve, 2000))
          }
          onSuccess={() => undefined}
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
    )
  })
