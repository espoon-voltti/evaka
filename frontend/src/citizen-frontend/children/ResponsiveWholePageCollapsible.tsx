// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useEffect, useState } from 'react'
import FocusLock from 'react-focus-lock'
import styled, { useTheme } from 'styled-components'

import { useTranslation } from 'citizen-frontend/localization'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import { tabletMin, tabletMinPx } from 'lib-components/breakpoints'
import {
  CollapsibleContentAreaProps,
  ContentArea,
  TitleContainer,
  TitleIcon
} from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import { H2, fontWeights } from 'lib-components/typography'
import { SpacingSize, defaultMargins } from 'lib-components/white-space'
import {
  faArrowLeft,
  faChevronDown,
  faChevronRight,
  faChevronUp
} from 'lib-icons'

const BreakingH2 = styled(H2)`
  word-break: break-word;
  hyphens: auto;
`

export default React.memo(function ResponsiveWholePageCollapsible({
  open,
  toggleOpen,
  title,
  children,
  countIndicator = 0,
  contentPadding = 's',
  ...props
}: Omit<CollapsibleContentAreaProps, 'title'> & {
  title: string
  contentPadding?: SpacingSize
}) {
  const { colors } = useTheme()

  const showCountIndicator =
    typeof countIndicator === 'number'
      ? countIndicator > 0
      : countIndicator.count > 0

  const t = useTranslation()

  const [width, setWidth] = useState(window.innerWidth)

  useEffect(() => {
    const onResize = () => {
      setWidth(window.innerWidth)
    }

    window.addEventListener('resize', onResize)

    return () => {
      window.removeEventListener('resize', onResize)
    }
  }, [])

  const [isFocusable, setIsFocusable] = useState(true)

  return (
    <ContentArea {...props} data-status={open ? 'open' : 'closed'}>
      <TitleContainer
        onClick={toggleOpen}
        data-qa={props['data-qa'] ? `${props['data-qa']}-header` : undefined}
        className={classNames({ open })}
        role="button"
        aria-expanded={open}
      >
        <BreakingH2 noMargin>{title}</BreakingH2>
        <FixedSpaceRow spacing="s">
          {showCountIndicator && (
            <RoundIcon
              size="m"
              color={colors.status.warning}
              content={(typeof countIndicator === 'number'
                ? countIndicator
                : countIndicator.count
              ).toString()}
              aria-label={
                typeof countIndicator === 'number'
                  ? undefined
                  : countIndicator.ariaLabel
              }
              data-qa="count-indicator"
            />
          )}
          {props.icon && <FontAwesomeIcon icon={props.icon} />}
          <div>
            <TabletAndDesktop>
              <TitleIcon
                icon={open ? faChevronUp : faChevronDown}
                data-qa="collapsible-trigger"
              />
            </TabletAndDesktop>
            <MobileOnly>
              <TitleIcon icon={faChevronRight} />
            </MobileOnly>
          </div>
        </FixedSpaceRow>
      </TitleContainer>
      <FocusLock disabled={!open || width >= tabletMinPx}>
        {open && (
          <ResponsiveCollapsibleContainer>
            <MobileOnly>
              <ResponsiveCollapsibleTitle>
                <FixedSpaceRow spacing="s">
                  <NonShrinkingIconButton
                    icon={faArrowLeft}
                    data-qa="return-collapsible"
                    onClick={() => toggleOpen()}
                    aria-label={t.common.return}
                  />
                  <div
                    tabIndex={isFocusable ? 0 : undefined}
                    onBlur={() => setIsFocusable(false)}
                    data-autofocus="true"
                  >
                    {title}
                  </div>
                </FixedSpaceRow>
              </ResponsiveCollapsibleTitle>
            </MobileOnly>
            <CollapsibleContainer padding={contentPadding}>
              {children}
            </CollapsibleContainer>
          </ResponsiveCollapsibleContainer>
        )}
      </FocusLock>
    </ContentArea>
  )
})

const ResponsiveCollapsibleContainer = styled.div`
  @media (max-width: ${tabletMin}) {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 29;
    background-color: ${(p) => p.theme.colors.grayscale.g0};
    margin-top: 0;
    display: flex;
    flex-direction: column;
  }
`

const ResponsiveCollapsibleTitle = styled.div`
  z-index: 30;
  top: 0;
  width: 100%;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0px 4px 4px rgba(0, 0, 0, 0.15);
  padding: ${defaultMargins.s};
  color: ${(p) => p.theme.colors.main.m1};
  font-weight: ${fontWeights.semibold};
  flex-shrink: 0;
`

const CollapsibleContainer = styled.div<{ padding: SpacingSize }>`
  margin-top: ${defaultMargins.s};

  @media (max-width: ${tabletMin}) {
    margin-top: 0;
    overflow-y: auto;
    flex-grow: 1;
    padding: 0 ${(p) => defaultMargins[p.padding]};
  }
`

const NonShrinkingIconButton = styled(IconButton)`
  flex-shrink: 0;
`
