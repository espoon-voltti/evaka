// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { desktopMin, tabletMin } from 'lib-components/breakpoints'
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
      {footerLogo ?? null}
    </FooterContainer>
  )
})

export const footerHeightDesktop = '72px'

const FooterItem = styled.div`
  display: inline-block;
  height: 40px;

  @media (min-width: ${desktopMin}) {
    height: auto;
  }
`

const FooterContainer = styled(Container)`
  display: flex;
  flex-direction: column;
  height: auto;
  align-items: left;
  margin: auto;
  padding: 30px 16px 20px 16px;
  font-weight: ${fontWeights.normal};

  @media (min-width: ${tabletMin}) {
    flex-wrap: wrap;
    max-height: 240px;
  }

  @media (min-width: ${desktopMin}) {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: start;
    padding-left: 96px;
    padding-right: 96px;
    height: ${footerHeightDesktop};
  }

  @media (min-width: 1408px) {
    padding-left: 32px;
    padding-right: 32px;
    justify-content: space-evenly;
  }

  a {
    display: inline-block;
    position: relative;
    padding-top: 8px;
    padding-bottom: 9px;
    margin-top: -8px;
    margin-bottom: -9px;
  }

  a:hover {
    text-decoration: underline;
  }
`
