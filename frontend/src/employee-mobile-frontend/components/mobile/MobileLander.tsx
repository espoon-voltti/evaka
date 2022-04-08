// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate } from 'react-router-dom'
import styled from 'styled-components'

import { P } from 'lib-components/typography'

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

  return renderResult(user, (u) => {
    if (u !== null) {
      if (u.unitIds.length === 1) {
        return (
          <Navigate
            replace
            to={`/units/${u.unitIds[0]}/groups/all/child-attendance/list/coming`}
          />
        )
      }

      return <Navigate replace to="/units" />
    }

    return (
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
  })
})
