// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import {
  faChevronUp,
  faChevronDown,
  fasChevronUp,
  fasChevronDown
} from 'lib-icons'

import { fontWeights } from '../typography'
import { defaultMargins, Gap } from '../white-space'

export const Table = styled.table`
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  color: ${(p) => p.theme.colors.grayscale.g100};
  width: 100%;
  border-collapse: collapse;
`

interface ThProps {
  sticky?: boolean
  top?: string
  hidden?: boolean
  align?: 'left' | 'right'
}

export const Th = styled.th<ThProps>`
  font-size: 14px;
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-weight: ${fontWeights.bold};
  line-height: 1.3em;
  text-transform: uppercase;
  vertical-align: middle;
  border-style: solid;
  border-color: ${(p) => p.theme.colors.grayscale.g15};
  border-width: 0 0 1px;
  padding: ${defaultMargins.s};
  text-align: ${({ align }) => align ?? 'left'};
  position: ${(p) => (p.sticky ? 'sticky' : 'static')};
  top: ${(p) => (p.sticky && p.top ? p.top : 'auto')};
  background: ${(p) => (p.sticky ? p.theme.colors.grayscale.g0 : 'none')};
`

export const Td = styled.td<{
  align?: 'right' | 'left'
  verticalAlign?: 'top' | 'middle' | 'bottom'
  color?: string
}>`
  line-height: 1.3em;
  border-style: solid;
  border-color: ${(p) => p.theme.colors.grayscale.g15};
  border-width: 0 0 1px;
  padding: ${defaultMargins.s};
  vertical-align: ${(p) => p.verticalAlign ?? 'top'};
  text-align: ${(p) => p.align ?? 'left'};
  ${(p) => (p.color ? `color: ${p.color};` : '')}
`

export interface TrProps {
  onClick?: () => void
}

export const Tr = styled.tr<TrProps>`
  ${(p) =>
    p.onClick
      ? `
    &:hover {
      box-shadow: 0 2px 6px 2px rgba(0, 0, 0, 0.1);
      transition: all 0.3s ease;
      cursor: pointer;
      user-select: none;
    }
  `
      : ''}
`

export const Thead = styled.thead``

export const Tbody = styled.tbody``

const SortableIconContainer = styled.div`
  display: flex;
  flex-direction: column;
`

interface SortableProps {
  children?: React.ReactNode
  onClick: () => void
  sorted?: 'ASC' | 'DESC'
  sticky?: boolean
  top?: string
}

const CustomButton = styled.button`
  display: flex;
  font-size: 14px;
  color: ${(p) => p.theme.colors.grayscale.g70};
  border: none;
  background: none;
  outline: none;
  padding: 0;
  margin: 0;
  cursor: pointer;
  text-transform: uppercase;
  font-weight: ${fontWeights.bold};
`

export const SortableTh = React.memo(function SortableTh({
  children,
  onClick,
  sorted,
  sticky,
  top
}: SortableProps) {
  const {
    colors: { grayscale }
  } = useTheme()
  return (
    <Th sticky={sticky} top={top}>
      <CustomButton onClick={onClick}>
        <span>{children}</span>
        <Gap horizontal size="xs" />
        <SortableIconContainer>
          <FontAwesomeIcon
            icon={sorted === 'ASC' ? fasChevronUp : faChevronUp}
            color={sorted === 'ASC' ? grayscale.g70 : grayscale.g35}
            size="xs"
          />
          <FontAwesomeIcon
            icon={sorted === 'DESC' ? fasChevronDown : faChevronDown}
            color={sorted === 'DESC' ? grayscale.g70 : grayscale.g35}
            size="xs"
          />
        </SortableIconContainer>
      </CustomButton>
    </Th>
  )
})
