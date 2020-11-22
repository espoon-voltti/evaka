// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import Colors from 'components/shared/Colors'

type Props = {
  align: 'center' | 'left' | 'right'
  fullWidth?: boolean
  shadow?: boolean
}

const alignValues = {
  center: 'center',
  left: 'flex-start',
  right: 'flex-end'
} as const

const StickyActionBar = styled.div<Props>`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: ${(props) => alignValues[props.align]};
  position: sticky;
  bottom: 0;
  background: white;
  padding: 16px 0;
  margin: ${(p) => (p.fullWidth ? '0 -9999rem' : '0')};
  margin-top: 50px;
  box-shadow: ${(p) =>
    p.shadow ? `0px -8px 8px -6px ${Colors.greyscale.lightest}` : 'inset'};
`

export default StickyActionBar
