// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { ContentArea } from 'lib-components/layout/Container'
import colors from 'lib-customizations/common'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faTimes } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { TallContentArea } from '../../mobile/components'

interface Props {
  loggedInStaffName: string
  logout: () => void
  cancel: () => void
}

export default React.memo(function PinLogout({
  loggedInStaffName,
  logout,
  cancel
}: Props) {
  const { i18n } = useTranslation()

  return (
    <FloatContainer>
      <TallContentArea
        opaque={false}
        paddingHorizontal={'zero'}
        paddingVertical={'zero'}
      >
        <ContentArea
          shadow
          opaque
          paddingHorizontal={'s'}
          paddingVertical={'m'}
        >
          <CancelContainer>
            <IconButton
              icon={faTimes}
              size={'m'}
              data-qa={'button-cancel-logout'}
              onClick={cancel}
            />
          </CancelContainer>
          <FixedSpaceColumn spacing={'m'}>
            <Key>{i18n.attendances.pin.staff}</Key>
            <span>{loggedInStaffName}</span>
            <LogoutButton data-qa="button-accept-logout" onClick={logout}>
              {i18n.attendances.pin.logOut}
            </LogoutButton>
          </FixedSpaceColumn>
        </ContentArea>
      </TallContentArea>
    </FloatContainer>
  )
})

const FloatContainer = styled.div`
  position: fixed;
  width: 100%;
  height: 100%;
  z-index: 1;
  top: 66px;
`

const CancelContainer = styled.div`
  display: flex;
  flex-direction: row-reverse;
`

const LogoutButton = styled.button`
  min-height: 45px;
  outline: none;
  cursor: pointer;
  font-family: 'Open Sans', sans-serif;
  font-size: 14px;
  line-height: 16px;
  font-weight: 600;
  text-transform: uppercase;
  white-space: nowrap;
  letter-spacing: 0.2px;
  width: 100%;

  display: flex;
  justify-content: center;
  align-items: center;
  color: ${colors.greyscale.white};
  background: ${colors.blues.primary};
`

const Key = styled.span`
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 4px;
`
