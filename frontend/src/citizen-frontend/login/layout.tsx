// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { css } from 'styled-components'

import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import { buttonBorderRadius } from 'lib-components/atoms/buttons/button-commons'
import { desktopMin } from 'lib-components/breakpoints'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faInfoCircle } from 'lib-icons'

import { useTranslation } from '../localization'
import { loginPageWidth } from '../page-widths'

export const TopGap = styled.div`
  height: ${defaultMargins.xs};

  @media (min-width: ${desktopMin}) {
    height: ${defaultMargins.L};
  }
`

export const LoginContainer = styled.div`
  margin: 0 auto;
  position: relative;

  @media (min-width: ${desktopMin}) {
    width: ${loginPageWidth};
    max-width: ${loginPageWidth};
  }
`

export const LoginCard = styled(ContentArea).attrs({ $opaque: true })`
  padding: ${defaultMargins.m};

  @media (min-width: ${desktopMin}) {
    padding: ${defaultMargins.XXL};
  }
`

export const LoginColumns = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.XXL};

  @media (min-width: ${desktopMin}) {
    flex-direction: row;
    gap: 0;

    > * {
      flex: 1 1 0;
      min-width: 0;
    }

    > :first-child {
      padding-right: ${defaultMargins.XXL};
    }

    > :last-child {
      padding-left: ${defaultMargins.XXL};
      border-left: 1px solid ${(p) => p.theme.colors.grayscale.g15};
    }
  }
`

export const LinkRow = styled.div`
  display: flex;
  gap: ${defaultMargins.L};
  padding: ${defaultMargins.m};

  @media (min-width: ${desktopMin}) {
    justify-content: center;
    padding: ${defaultMargins.m} 0;
  }
`

export const rowLinkStyles = css`
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  font-weight: ${fontWeights.semibold};
`

const RowLink = styled.a`
  ${rowLinkStyles}
`

export const HelpLink = React.memo(function HelpLink() {
  const i18n = useTranslation()
  const url = i18n.loginPage.helpUrl
  if (url === null) return null
  return (
    <RowLink
      href={url}
      target="_blank"
      rel="noreferrer"
      data-qa="login-help-link"
    >
      <FontAwesomeIcon icon={faInfoCircle} />
      <Gap $size="xs" $horizontal />
      {i18n.loginPage.helpLink}
    </RowLink>
  )
})

export const HelpLinkRow = React.memo(function HelpLinkRow() {
  const i18n = useTranslation()
  if (i18n.loginPage.helpUrl === null) return null
  return (
    <LinkRow>
      <HelpLink />
    </LinkRow>
  )
})

const wideButtonStyles = css`
  width: 100%;
  height: 52px;
`

export const WideLinkButton = styled(LinkButton)`
  ${wideButtonStyles}
`

export const WideAsyncButton = styled(AsyncButton)`
  ${wideButtonStyles}
`

export const UsedLastChip = styled.div`
  align-self: flex-start;
  background-color: ${(p) => p.theme.colors.main.m4};
  color: ${(p) => p.theme.colors.main.m1};
  font-family: 'Open Sans', sans-serif;
  font-size: 0.875rem;
  line-height: 1.25rem;
  font-weight: ${fontWeights.semibold};
  /* The extra bottom padding slides under the button, so the button's rounded
     top corner shows chip colour instead of the card background. */
  padding: ${defaultMargins.xxs} ${defaultMargins.xs}
    calc(${defaultMargins.xxs} + ${buttonBorderRadius});
  margin-bottom: -${buttonBorderRadius};
  border-radius: ${buttonBorderRadius} ${buttonBorderRadius} 0 0;
`

const TwoLineButtonRoot = styled.button<{ $primary: boolean }>`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: ${defaultMargins.x3s};
  width: 100%;
  min-height: 80px;
  padding: ${defaultMargins.s} ${defaultMargins.m};
  border: 1px solid ${(p) => p.theme.colors.main.m2};
  border-radius: ${buttonBorderRadius};
  cursor: pointer;
  outline: none;
  text-align: center;
  font-family: 'Open Sans', sans-serif;
  letter-spacing: 0.2px;
  background-color: ${(p) =>
    p.$primary ? p.theme.colors.main.m2 : p.theme.colors.grayscale.g0};
  color: ${(p) =>
    p.$primary ? p.theme.colors.grayscale.g0 : p.theme.colors.main.m2};

  &:hover {
    border-color: ${(p) => p.theme.colors.main.m2Hover};
    background-color: ${(p) =>
      p.$primary ? p.theme.colors.main.m2Hover : p.theme.colors.grayscale.g0};
    color: ${(p) =>
      p.$primary ? p.theme.colors.grayscale.g0 : p.theme.colors.main.m2Hover};
  }

  &:focus {
    box-shadow:
      0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
      0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
  }
`

const TwoLineButtonLabel = styled.span`
  font-size: 1rem;
  line-height: 1.5rem;
  font-weight: ${fontWeights.semibold};
`

const TwoLineButtonDescription = styled.span`
  font-size: 0.875rem;
  line-height: 1.5rem;
  font-weight: ${fontWeights.normal};
`

const DesktopText = styled.span`
  display: none;

  @media (min-width: ${desktopMin}) {
    display: inline;
  }
`

const MobileText = styled.span`
  display: inline;

  @media (min-width: ${desktopMin}) {
    display: none;
  }
`

export const TwoLineButton = React.memo(function TwoLineButton({
  label,
  descriptionDesktop,
  descriptionMobile,
  primary,
  onClick,
  'data-qa': dataQa
}: {
  label: string
  descriptionDesktop: string
  descriptionMobile: string
  primary: boolean
  onClick: () => void
  'data-qa'?: string
}) {
  return (
    <TwoLineButtonRoot
      type="button"
      $primary={primary}
      onClick={onClick}
      data-qa={dataQa}
    >
      <TwoLineButtonLabel>{label}</TwoLineButtonLabel>
      <TwoLineButtonDescription>
        <DesktopText>{descriptionDesktop}</DesktopText>
        <MobileText>{descriptionMobile}</MobileText>
      </TwoLineButtonDescription>
    </TwoLineButtonRoot>
  )
})
