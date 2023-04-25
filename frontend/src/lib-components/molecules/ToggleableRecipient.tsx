// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTimes } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import type { UUID } from 'lib-common/types'

import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'

import type { SelectableAccount } from './MessageReplyEditor'

interface ToggleableRecipientProps {
  'data-qa'?: string
  recipient: SelectableAccount
  onToggleRecipient: (id: UUID, selected: boolean) => void
  labelAdd: string
}

const Recipient = styled.button<{ selected: boolean; toggleable: boolean }>`
  cursor: ${(p) => (p.toggleable ? 'pointer' : 'default')};;
  padding: 0 ${(p) => (p.selected ? '12px' : defaultMargins.xs)};
  background-color: ${(p) =>
    p.selected ? p.theme.colors.grayscale.g15 : 'unset'};
  border-radius: 1000px;
  border-width: 0;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => (p.selected ? 'unset' : p.theme.colors.main.m2)};

  & > :last-child {
    margin-left: ${defaultMargins.xs};
  }

  :not(:last-child) {
    margin-right: ${defaultMargins.xs};
`

export function ToggleableRecipient({
  'data-qa': dataQa,
  recipient: { id, name, selected, toggleable },
  labelAdd,
  onToggleRecipient
}: ToggleableRecipientProps) {
  const onClick = toggleable
    ? () => onToggleRecipient(id, !selected)
    : undefined

  return (
    <Recipient
      onClick={onClick}
      selected={selected}
      toggleable={toggleable}
      disabled={!toggleable}
      data-qa={dataQa}
    >
      {selected ? (
        <>
          {name}
          {toggleable && <FontAwesomeIcon icon={faTimes} />}
        </>
      ) : (
        `+ ${labelAdd} ${name}`
      )}
    </Recipient>
  )
}
