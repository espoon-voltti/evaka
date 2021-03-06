// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Action } from './ApplicationActions'
import React from 'react'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import styled from 'styled-components'

type Props = {
  action: Action | undefined
}

const PrimaryActionContainer = styled.div`
  padding-right: 16px;
`

export default React.memo(function PrimaryAction({ action }: Props) {
  return (
    <>
      {action && (
        <PrimaryActionContainer onClick={(e) => e.stopPropagation()}>
          <InlineButton
            text={action.label}
            onClick={action.onClick}
            disabled={!action.enabled}
            data-qa={`primary-action-${action.id}`}
          />
        </PrimaryActionContainer>
      )}
    </>
  )
})
