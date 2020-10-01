// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { StylesConfig } from 'react-select'

export interface BaseProps {
  className?: string
  dataQa?: string
}

export const reactSelectStyles: StylesConfig = {
  menu: (styles) => {
    return {
      ...styles,
      zIndex: 4
    }
  }
}
