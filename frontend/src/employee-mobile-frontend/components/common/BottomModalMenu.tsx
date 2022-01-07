// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTimes } from 'lib-icons'
import React from 'react'
import styled from 'styled-components'
import ModalBackground from 'lib-components/molecules/modals/ModalBackground'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'

const MenuBackground = styled.div`
  z-index: 100;
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  min-height: 20%;
  background-color: ${colors.greyscale.white};
  box-shadow: 0 -4px 4px rgba(0, 0, 0, 0.1);
  padding: 24px 16px;
`

const TopRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  color: ${colors.main.dark};
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  line-height: 24px;
`

interface Props {
  title: string
  onClose: () => void
  children?: React.ReactNode
}
function BottomModalMenu({ title, onClose, children }: Props) {
  return (
    <ModalBackground>
      <MenuBackground>
        <TopRow>
          <span>{title}</span>
          <FontAwesomeIcon icon={faTimes} onClick={onClose} />
        </TopRow>
        <div>{children}</div>
      </MenuBackground>
    </ModalBackground>
  )
}

export default BottomModalMenu
