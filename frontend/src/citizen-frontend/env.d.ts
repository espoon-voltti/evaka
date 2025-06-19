// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/// <reference types="vite/client" />

import type { Theme } from 'lib-common/theme'

declare module 'styled-components' {
  export interface DefaultTheme extends Theme {}
}
