// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { desktopMin } from '@evaka/lib-components/src/breakpoints'
import EspooLogo from './EspooLogo'
import EvakaLogo from './EvakaLogo'
import DesktopNav from './DesktopNav'
import MobileNav from './MobileNav'
import { headerHeight } from '../header/const'
import { Result } from '@evaka/lib-common/src/api'
import { getUnreadBulletinsCount } from '~messages/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { useUser } from '../auth'

export default React.memo(function Header() {
  const [showMenu, setShowMenu] = useState(false)
  const user = useUser()
  const [
    unreadBulletinsCount,
    setUnreadBulletinsCount
  ] = useState<Result<number> | null>(null)

  const loadUnreadCount = useRestApi(
    getUnreadBulletinsCount,
    setUnreadBulletinsCount
  )
  useEffect(() => {
    if (user) loadUnreadCount()
  }, [user])

  return (
    <HeaderContainer fixed={showMenu}>
      <EspooLogo />
      <EvakaLogo />
      <DesktopNav
        unreadMessagesCount={
          unreadBulletinsCount?.isSuccess ? unreadBulletinsCount.value : 0
        }
      />
      <MobileNav
        showMenu={showMenu}
        setShowMenu={setShowMenu}
        unreadMessagesCount={
          unreadBulletinsCount?.isSuccess ? unreadBulletinsCount.value : 0
        }
      />
    </HeaderContainer>
  )
})

const HeaderContainer = styled.header<{ fixed: boolean }>`
  z-index: 99;
  color: ${colors.greyscale.white};
  background-color: ${colors.blues.primary};
  position: ${({ fixed }) => (fixed ? 'fixed' : 'relative')};
  display: grid;
  grid: minmax(60px, min-content) / repeat(3, minmax(100px, 1fr));
  height: 60px;
  width: 100%;

  @media (min-width: ${desktopMin}) {
    grid: minmax(${headerHeight}, min-content) / max-content max-content auto;
    height: ${headerHeight};
  }
`
