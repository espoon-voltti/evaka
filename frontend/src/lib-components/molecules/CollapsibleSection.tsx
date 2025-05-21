// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import { faAngleDown, faAngleUp } from 'lib-icons'

import { desktopMin } from '../breakpoints'
import { H3 } from '../typography'
import type { BaseProps } from '../utils'
import { Gap, defaultMargins } from '../white-space'

const Wrapper = styled.div`
  width: 100%;
  margin-bottom: ${defaultMargins.XL};

  &.fitted {
    margin-bottom: 0;
  }
`

const IconWrapper = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g35};
  font-size: 28px;
  width: 35px;
  margin-right: ${defaultMargins.s};
  display: flex;
  justify-content: center;
  align-items: flex-end;
`

const Row = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.m};

  &.fitted {
    margin-bottom: 0;
  }

  &:hover {
    cursor: pointer;
  }
`

const ToggleWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  margin-left: ${defaultMargins.s};
  font-size: 24px;
  @media screen and (min-width: ${desktopMin}) {
    font-size: 32px;
  }
`

const Content = styled.div`
  &.collapsed {
    display: none;
  }
`

type CollapsibleSectionProps = BaseProps & {
  title: string
  icon?: IconDefinition
  children: React.ReactNode
  startCollapsed?: boolean
  fitted?: boolean
  'data-qa'?: string
}

/**
 * @deprecated use CollapsibleContentArea instead
 */
export default React.memo(function CollapsibleSection({
  title,
  icon,
  children,
  startCollapsed = false,
  fitted = false,
  className,
  'data-qa': dataQa
}: CollapsibleSectionProps) {
  const { colors } = useTheme()
  const [collapsed, setCollapsed] = useState<boolean>(startCollapsed)
  const toggleCollapse = useCallback(() => {
    setCollapsed((prev) => !prev)
  }, [])

  return (
    <Wrapper
      className={classNames(className, { fitted })}
      data-qa={dataQa}
      data-status={collapsed ? 'closed' : 'open'}
    >
      <Row
        onClick={toggleCollapse}
        className={classNames(className, { fitted })}
      >
        <div>
          {icon && (
            <IconWrapper>
              <FontAwesomeIcon icon={icon} />
            </IconWrapper>
          )}
          {fitted && icon && <Gap horizontal size="xs" />}
          <H3 fitted noMargin={fitted}>
            {title}
          </H3>
        </div>
        <ToggleWrapper data-qa="collapsible-trigger">
          <FontAwesomeIcon
            icon={collapsed ? faAngleDown : faAngleUp}
            color={colors.main.m2}
          />
        </ToggleWrapper>
      </Row>
      <Content className={classNames({ collapsed })}>{children}</Content>
    </Wrapper>
  )
})
