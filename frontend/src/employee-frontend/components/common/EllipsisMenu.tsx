// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useBoolean } from 'lib-common/form/hooks'
import { useCloseOnOutsideEvent } from 'lib-common/utils/useCloseOnOutsideEvent'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
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
  const [isMenuOpen, { toggle: toggleMenu, off: closeMenu }] = useBoolean(false)
  const containerRef = useCloseOnOutsideEvent(isMenuOpen, closeMenu)

  const { i18n } = useTranslation()

  return (
    <Container ref={containerRef}>
      {items.length > 0 && (
        <>
          <IconOnlyButton
            size="m"
            icon={faEllipsisVAlt}
            onClick={(e: React.MouseEvent<HTMLButtonElement>) => {
              toggleMenu()
              e.stopPropagation()
            }}
            data-qa={dataQa}
            aria-label={isMenuOpen ? i18n.common.close : i18n.common.open}
          />
          {isMenuOpen && <MenuList items={items} closeMenu={closeMenu} />}
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
  return (
    <Menu onClick={(e) => e.stopPropagation()}>
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

const MenuItem = styled(LegacyInlineButton)`
  color: ${({ theme }) => theme.colors.grayscale.g100};
  font-weight: 400;
  white-space: nowrap;
  margin: ${defaultMargins.xs};
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.grayscale.g70};
  }
`
