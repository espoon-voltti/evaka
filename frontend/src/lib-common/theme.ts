// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AbsenceType } from './generated/api-types/absence'

export interface Theme {
  colors: {
    main: {
      /** Main 1 (Dark variant) */
      m1: string
      /** Main 2 (Primary) */
      m2: string
      /** Main 2 (Primary hover) */
      m2Hover: string
      /** Main 2 (Primary active) */
      m2Active: string
      /** Main 2 (Primary focus) */
      m2Focus: string
      /** Main 3 (Light variant) */
      m3: string
      /** Main 4 (Lighter variant) */
      m4: string
    }
    grayscale: {
      /** Grayscale-100 */
      g100: string
      /** Grayscale-70 */
      g70: string
      /** Grayscale-35 */
      g35: string
      /** Grayscale-15 */
      g15: string
      /** Grayscale-4 */
      g4: string
      /** Grayscale-0 */
      g0: string
    }
    status: {
      danger: string
      warning: string
      success: string
      info: string
    }
    accents: {
      a1greenDark: string
      a2orangeDark: string
      a3emerald: string
      a4violet: string
      a5orangeLight: string
      a6turquoise: string
      a7mint: string
      a8lightBlue: string
      a9pink: string
      a10powder: string
    }
    absences?: Partial<Record<AbsenceType, string>>
  }
  typography: {
    h1: FontSettings & HasBoldOption
    h2: FontSettings & HasBoldOption
    h3: FontSettings & HasBoldOption
    h4: FontSettings & HasBoldOption
  }
}

type FontSettings = {
  weight: FontWeight
  mobile?: Omit<FontSettings, 'mobile'>
}
type HasBoldOption = { bold: FontWeight }

type FontWeight =
  | 'normal'
  | 'bold'
  | 'lighter'
  | 'bolder'
  | number
  | 'inherit'
  | 'initial'
  | 'unset'
