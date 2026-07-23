// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { Button } from '../atoms/buttons/Button'

export const ScreenReaderButton = styled(Button)`
  position: absolute;
  z-index: 10000;
  left: -10000px;

  &:focus {
    left: auto;
  }
`
