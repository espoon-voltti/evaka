// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { desktopMin } from 'lib-components/breakpoints'
import Container from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'
import { footerLogo } from 'lib-customizations/citizen'

import { useTranslation } from './localization'

export const FooterContent = React.memo(function FooterContent() {
  const t = useTranslation()
  return (
    <>
      <FooterItem data-qa="footer-citylabel">{t.footer.cityLabel}</FooterItem>
      <FooterItem>{t.footer.privacyPolicyLink}</FooterItem>
      <FooterItem>
        <Link to="/accessibility">{t.footer.accessibilityStatement}</Link>
      </FooterItem>
      <FooterItem>{t.footer.sendFeedbackLink}</FooterItem>
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

export const footerHeightDesktop = '62px'

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
  grid-template-columns: 1fr repeat(4, auto) 1fr;
  grid-column-gap: 80px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  padding: 20px 0 20px 0;
  font-weight: ${fontWeights.normal};

  ${FooterItem}:nth-child(1) {
    grid-column-start: 2;
    @media (max-width: ${desktopMin}) {
      grid-column-start: 1;
    }
  }

  @media (max-width: ${desktopMin}) {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }

  a:hover {
    text-decoration: underline;
  }
`
