// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'styled-components'
import { Theme } from 'lib-common/theme'

declare module 'styled-components' {
  export interface DefaultTheme extends Theme {}
}
