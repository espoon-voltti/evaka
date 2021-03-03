// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from './localization'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'

export const FooterContent = React.memo(function FooterContent() {
  const t = useTranslation()
  return (
    <>
      <FooterItem>{t.footer.espooLabel}</FooterItem>
      <FooterItem>
        <FooterLink href={t.footer.privacyPolicyLink} data-qa={'nav-old-map'}>
          {t.footer.privacyPolicy}
        </FooterLink>
      </FooterItem>
      <FooterItem>
        <FooterLink href={t.footer.sendFeedbackLink} data-qa={'nav-old-map'}>
          {t.footer.sendFeedback}
        </FooterLink>
      </FooterItem>
    </>
  )
})

export default React.memo(function Footer() {
  return (
    <FooterContainer>
      <FooterContent />
    </FooterContainer>
  )
})

const FooterContainer = styled.footer`
  display: flex;
  flex-direction: row;
  justify-content: center;
  flex-wrap: wrap;
  padding: 20px 0 20px 0;
  font-size: 12px;
  font-weight: 400;
`

const FooterItem = styled.div`
  display: inline-block;
  margin: 0 40px 0 40px;
`

const FooterLink = styled.a`
  color: ${colors.brandEspoo.espooBlue};
  text-decoration: none;
`
