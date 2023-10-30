// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import IconButton from '../atoms/buttons/IconButton'
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
export const DeleteThreadButton = styled(IconButton)``
export const Container = styled.div<{ isRead: boolean; active: boolean }>`
  display: block;
  border: 0;

  text-align: left;
  width: 100%;

  background-color: ${(p) => p.theme.colors.grayscale.g0};
  color: ${(p) => p.theme.colors.grayscale.g100};
  padding: ${defaultMargins.s};
  cursor: pointer;
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  position: relative;

  &:focus {
    outline: none;
    border: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    padding: calc(${defaultMargins.s} - 1px) calc(${defaultMargins.m} - 2px);
  }

  @media (pointer: coarse) {
    ${DeleteThreadButton} {
      display: none;
    }
  }
  @media (pointer: fine) {
    ${DeleteThreadButton} {
      opacity: 0;
    }

    ${DeleteThreadButton}:focus {
      opacity: 1;
    }
    &:hover {
      ${DeleteThreadButton} {
        opacity: 1;
      }
    }
  }

  ${(p) =>
    !p.isRead
      ? `
    &:before {
      background-color: ${p.theme.colors.status.success};
      width: 6px;
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      content: '';
    }
  `
      : ''}

  ${(p) => (p.active ? `background-color: ${p.theme.colors.main.m4};` : '')}
`

export const ThreadContainer = styled.div`
  width: 100%;
  box-sizing: border-box;
  min-width: 300px;
  max-width: 100%;
  min-height: 500px;
  overflow-y: auto;

  a {
    text-decoration: underline;
  }
`
