// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
      /** Accent 5 */
      a1greenDark: string
      /** Accent 6 */
      a2orangeDark: string
      /** Accent 7 */
      a3emerald: string
      /** Accent 8 */
      a4violet: string
      /** Accent 9 */
      a5orangeLight: string
      /** Accent 10 */
      a6turquoise: string
      /** Accent 11 */
      a7mint: string
      /** Accent 12 */
      a8lightBlue: string
      /** Accent 13 */
      a9pink: string
    }
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
