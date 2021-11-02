// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { P } from 'lib-components/typography'
import React, { useContext } from 'react'
import { Redirect } from 'react-router-dom'
import styled from 'styled-components'
import EvakaLogo from '../../assets/EvakaLogo.svg'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { FullHeightContainer, WideLinkButton } from './components'

const Top = styled.div``

const Bottom = styled.div``

const Img = styled.img`
  max-width: 100%;
`

export default React.memo(function MobileLander() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  return renderResult(user, (u) =>
    u?.unitId ? (
      <Redirect
        to={`/units/${u.unitId}/groups/all/child-attendance/list/coming`}
      />
    ) : (
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
    )
  )
})
