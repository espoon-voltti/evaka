// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'

export const Header = styled.div<{ isRead: boolean }>`
  display: flex;
  justify-content: space-between;
  font-weight: ${({ isRead }) =>
    isRead ? fontWeights.normal : fontWeights.semibold};
  font-size: 16px;
  margin-bottom: 12px;
`
export const TitleAndDate = styled.div<{ isRead: boolean }>`
  display: flex;
  justify-content: space-between;
  font-weight: ${({ isRead }) =>
    isRead ? fontWeights.normal : fontWeights.semibold};
  margin-bottom: ${defaultMargins.xxs};
`
export const Truncated = styled.span`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  :not(:last-child) {
    margin-right: ${defaultMargins.s};
  }
`
export const Container = styled.div<{ isRead: boolean; active: boolean }>`
  text-align: left;
  width: 100%;

  background-color: ${({ theme: { colors } }) => colors.greyscale.white};
  padding: ${defaultMargins.s} ${defaultMargins.m};
  cursor: pointer;

  border: 1px solid ${({ theme: { colors } }) => colors.greyscale.lighter};

  &:focus {
    border: 2px solid ${({ theme: { colors } }) => colors.accents.petrol};
    margin: -1px 0;
    padding: ${defaultMargins.s} calc(${defaultMargins.m} - 1px);
  }

  ${(p) =>
    !p.isRead
      ? `
    border-left-color: ${p.theme.colors.brand.secondary} !important;
    border-left-width: 6px !important;
    padding-left: calc(${defaultMargins.m} - 6px) !important;
  `
      : ''}

  ${(p) =>
    p.active ? `background-color: ${p.theme.colors.brand.secondaryLight};` : ''}
`

export const ThreadContainer = styled.div`
  width: 100%;
  box-sizing: border-box;
  min-width: 300px;
  max-width: 100%;
  min-height: 500px;
  overflow-y: auto;
`
