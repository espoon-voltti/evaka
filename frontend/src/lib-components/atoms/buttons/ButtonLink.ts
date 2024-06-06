// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

/**
 * @deprecated use Button and appearance="link" instead
 */
export const ButtonLink = styled.button`
  color: ${(p) => p.theme.colors.main.m2};
  cursor: pointer;
  text-decoration: underline;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  display: inline;
  text-align: left;

  &:hover {
    text-decoration: none;
    color: ${(p) => p.theme.colors.main.m1};
  }
`
