// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from './localization'
import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { footerLogo } from 'lib-customizations/citizen'
import { desktopMin } from 'lib-components/breakpoints'
import Container from 'lib-components/layout/Container'

export const FooterContent = React.memo(function FooterContent() {
  const t = useTranslation()
  return (
    <>
      <FooterItem>{t.footer.cityLabel}</FooterItem>
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
    <FooterContainer as="footer">
      <FooterContent />
      {footerLogo ? (
        <LogoItem>
          <img src={footerLogo.src} alt={footerLogo.alt} />
        </LogoItem>
      ) : null}
    </FooterContainer>
  )
})

const FooterItem = styled.div`
  display: inline-block;
  margin: auto;
`

const LogoItem = styled.div`
  margin-left: auto;
`

const FooterContainer = styled(Container)`
  position: static;
  display: grid;
  grid-template-columns: 1fr repeat(3, auto) 1fr;
  grid-column-gap: 80px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  padding: 20px 0 20px 0;
  font-size: 12px;
  font-weight: 400;
  ${FooterItem}:nth-child(1) {
    grid-column-start: 2;
    @media (max-width: ${desktopMin}) {
      grid-column-start: 1;
    }
  }
  @media (max-width: ${desktopMin}) {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }
`

const FooterLink = styled.a`
  color: ${colors.brandEspoo.espooBlue};
  text-decoration: none;
`
