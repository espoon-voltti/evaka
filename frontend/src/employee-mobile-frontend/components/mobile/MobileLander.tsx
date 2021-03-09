// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import { useContext } from 'react'
import { Redirect } from 'react-router-dom'
import styled from 'styled-components'

import { P } from '@evaka/lib-components/typography'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import EvakaLogo from '../../assets/EvakaLogo.svg'
import { FullHeightContainer, WideLinkButton } from './components'

const Top = styled.div``

const Bottom = styled.div``

const Img = styled.img`
  max-width: 100%;
`

export default React.memo(function MobileLander() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  if (user?.unitId) {
    return <Redirect to={`/units/${user.unitId}/attendance/all/coming`} />
  }

  return (
    <Fragment>
      <FullHeightContainer spaced>
        <Top>
          <Img src={EvakaLogo} />
          <section>
            <P centered>{i18n.mobile.landerText1}</P>
            <P centered>{i18n.mobile.landerText2}</P>
          </section>
        </Top>

        <Bottom>
          <WideLinkButton to="/pairing" data-qa="start-pairing-btn">
            {i18n.mobile.actions.ADD_DEVICE.toUpperCase()}
          </WideLinkButton>
        </Bottom>
      </FullHeightContainer>
    </Fragment>
  )
})
