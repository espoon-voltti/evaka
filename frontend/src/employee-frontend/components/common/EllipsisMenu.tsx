// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { defaultMargins } from 'lib-components/white-space'
import { faEllipsisVAlt } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

interface Props {
  items: MenuItem[]
  ['data-qa']?: string
}

export interface MenuItem {
  id: string
  label: string
  onClick: () => void
  disabled?: boolean
}

export default React.memo(function EllipsisMenu({
  items,
  ['data-qa']: dataQa
}: Props) {
  const [showMenu, setShowMenu] = useState(false)
  const closeMenu = () => setShowMenu(false)

  const { i18n } = useTranslation()

  return (
    <Container>
      {items.length > 0 && (
        <>
          <IconButton
            size="m"
            icon={faEllipsisVAlt}
            onClick={(e: React.MouseEvent<HTMLButtonElement>) => {
              setShowMenu((value) => !value)
              e.stopPropagation()
            }}
            data-qa={dataQa}
            aria-label={showMenu ? i18n.common.close : i18n.common.open}
          />
          {showMenu && <MenuList items={items} closeMenu={closeMenu} />}
        </>
      )}
    </Container>
  )
})

const Container = styled.div`
  position: relative;
`

type MenuListProps = {
  items: MenuItem[]
  closeMenu: () => void
}

const MenuList = React.memo(function MenuList({
  items,
  closeMenu
}: MenuListProps) {
  const containerRef = useCloseOnOutsideClick<HTMLDivElement>(closeMenu)

  return (
    <Menu ref={containerRef} onClick={(e) => e.stopPropagation()}>
      {items.map(({ id, label, onClick, disabled }) => (
        <MenuItem
          key={id}
          onClick={() => {
            onClick()
            closeMenu()
          }}
          disabled={disabled}
          data-qa={`menu-item-${id}`}
          text={label}
        />
      ))}
    </Menu>
  )
})

const Menu = styled.div`
  position: absolute;
  top: 0;
  right: 40px;
  z-index: 2;
  background: ${({ theme }) => theme.colors.grayscale.g0};
  box-shadow: 0 2px 6px 2px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 10px;
  cursor: auto;
`

const MenuItem = styled(InlineButton)`
  color: ${({ theme }) => theme.colors.grayscale.g100};
  font-weight: 400;
  white-space: nowrap;
  margin: ${defaultMargins.xs};
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.grayscale.g70};
  }
`
