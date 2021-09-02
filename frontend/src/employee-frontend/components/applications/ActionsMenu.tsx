// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { faEllipsisVAlt } from 'lib-icons'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import colors from 'lib-customizations/common'
import { Action } from './ApplicationActions'

type Props = {
  actions: Action[]
}

export default React.memo(function ActionsMenu({ actions }: Props) {
  const [showMenu, setShowMenu] = useState(false)
  const closeMenu = () => setShowMenu(false)

  return (
    <Container>
      {actions.length > 0 && (
        <>
          <IconButton
            size={'m'}
            icon={faEllipsisVAlt}
            onClick={(e: React.MouseEvent<HTMLButtonElement>) => {
              setShowMenu((value) => !value)
              e.stopPropagation()
            }}
            data-qa="application-actions-menu"
          />
          {showMenu && <MenuList actions={actions} closeMenu={closeMenu} />}
        </>
      )}
    </Container>
  )
})

const Container = styled.div`
  position: relative;
`

type MenuListProps = {
  actions: Action[]
  closeMenu: () => void
}

const MenuList = React.memo(function MenuList({
  actions,
  closeMenu
}: MenuListProps) {
  const containerRef = useCloseOnOutsideClick<HTMLDivElement>(closeMenu)

  return (
    <Menu ref={containerRef} onClick={(e) => e.stopPropagation()}>
      {actions
        .filter((action) => !action.disabled)
        .map(({ id, label, onClick }) => (
          <MenuItem key={id} onClick={onClick} data-qa={`action-item-${id}`}>
            {label}
          </MenuItem>
        ))}
    </Menu>
  )
})

const Menu = styled.div`
  position: absolute;
  top: 0;
  right: 40px;
  z-index: 2;
  background: white;
  box-shadow: 0 2px 6px 2px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 10px;
  cursor: auto;
`

const MenuItem = styled.span`
  white-space: nowrap;
  margin: 10px;
  cursor: pointer;

  &:hover {
    color: ${colors.greyscale.medium};
  }
`
