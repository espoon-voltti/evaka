// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronUp as falChevronUp } from '@evaka/icons'
import { faChevronDown as falChevronDown } from '@evaka/icons'
import { faChevronUp as fasChevronUp } from '@evaka/icons'
import { faChevronDown as fasChevronDown } from '@evaka/icons'
import { DefaultMargins, Gap } from 'components/shared/layout/white-space'
import { Greyscale } from 'components/shared/Colors'

export const Table = styled.table`
  background-color: ${Greyscale.white};
  color: ${Greyscale.darkest};
  width: 100%;
  border-collapse: separate;
`

interface ThProps {
  sticky?: boolean
  top?: string
}

export const Th = styled.th<ThProps>`
  font-size: 14px;
  color: ${Greyscale.dark};
  font-weight: 700;
  line-height: 1.3em;
  text-transform: uppercase;
  vertical-align: middle;
  border-style: solid;
  border-color: ${Greyscale.lighter};
  border-width: 0 0 1px;
  padding: ${DefaultMargins.s};
  text-align: left;
  position: ${(p) => (p.sticky ? 'sticky' : 'static')};
  top: ${(p) => (p.sticky && p.top ? p.top : 'auto')};
  background: ${(p) => (p.sticky ? Greyscale.white : 'none')};
`

export const Td = styled.td`
  line-height: 1.3em;
  border-style: solid;
  border-color: ${Greyscale.lighter};
  border-width: 0 0 1px;
  padding: ${DefaultMargins.s};
  vertical-align: top;
  text-align: left;
`

interface TrProps {
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
  color: ${Greyscale.dark};
  border: none;
  background: none;
  outline: none;
  padding: 0;
  margin: 0;
  cursor: pointer;
  text-transform: uppercase;
  font-weight: 700;
`

export const SortableTh = ({
  children,
  onClick,
  sorted,
  sticky,
  top
}: SortableProps) => (
  <Th sticky={sticky} top={top}>
    <CustomButton onClick={onClick}>
      <span>{children}</span>
      <Gap horizontal size="xs" />
      <SortableIconContainer>
        <FontAwesomeIcon
          icon={sorted === 'ASC' ? fasChevronUp : falChevronUp}
          color={sorted === 'ASC' ? Greyscale.dark : Greyscale.medium}
          size="xs"
        />
        <FontAwesomeIcon
          icon={sorted === 'DESC' ? fasChevronDown : falChevronDown}
          color={sorted === 'DESC' ? Greyscale.dark : Greyscale.medium}
          size="xs"
        />
      </SortableIconContainer>
    </CustomButton>
  </Th>
)
