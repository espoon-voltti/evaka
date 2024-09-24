// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { fontWeights } from 'lib-components/typography'

import { buttonBorderRadius } from './button-commons'

export default styled.a`
  -webkit-font-smoothing: antialiased;
  text-size-adjust: 100%;
  box-sizing: inherit;
  height: 45px;
  padding: 0 27px;
  width: fit-content;
  min-width: 100px;
  text-align: center;
  overflow-x: hidden;
  border: 1px solid ${(p) => p.theme.colors.main.m2};
  border-radius: ${buttonBorderRadius};
  outline: none;
  cursor: pointer;
  font-family: 'Open Sans', sans-serif;
  font-size: 1rem;
  line-height: 1rem;
  font-weight: ${fontWeights.semibold};
  white-space: nowrap;
  letter-spacing: 0.2px;
  color: ${(p) => p.theme.colors.grayscale.g0};
  background-color: ${(p) => p.theme.colors.main.m2};
  margin-right: 0;
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;

  &:hover {
    background-color: ${(p) => p.theme.colors.main.m2Hover};
  }
  &:focus {
    box-shadow:
      0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
      0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
  }
  &:active {
    background-color: ${(p) => p.theme.colors.main.m2Active};
  }
`
