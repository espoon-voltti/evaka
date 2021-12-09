// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type IconSize = 'xs' | 's' | 'm' | 'L' | 'XL' | 'XXL'

export const fontSizeByIconSize = (size: IconSize): number => {
  switch (size) {
    case 'xs':
      return 8
    case 's':
      return 12
    case 'm':
      return 16
    case 'L':
      return 18
    case 'XL':
      return 44
    case 'XXL':
      return 80
  }
}

export const diameterByIconSize = (size: IconSize): number => {
  switch (size) {
    case 'xs':
      return 16
    case 's':
      return 20
    case 'm':
      return 24
    case 'L':
      return 34
    case 'XL':
      return 64
    case 'XXL':
      return 128
  }
}
