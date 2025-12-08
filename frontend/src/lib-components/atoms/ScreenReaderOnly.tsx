// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { isAutomatedTest } from 'lib-common/utils/helpers'

export const ScreenReaderOnly = isAutomatedTest
  ? styled.div`
      display: none;
    `
  : styled.div`
      position: absolute;
      left: -10000px;
      top: auto;
      width: 1px;
      height: 1px;
      overflow: hidden;
    `

export const ScreenReaderOnlyInline = isAutomatedTest
  ? styled.div`
      display: none;
    `
  : styled.span`
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      border: 0;
      clip: rect(0 0 0 0);
      clip-path: inset(50%);
      margin: -1px;
      padding: 0;
      white-space: nowrap; /* prevent line breaks */
    `
