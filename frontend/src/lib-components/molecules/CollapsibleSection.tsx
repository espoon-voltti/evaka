// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleDown, faAngleUp } from 'lib-icons'
import colors from '../colors'
import { H3 } from '../typography'
import { defaultMargins } from '../white-space'
import { BaseProps } from '../utils'

const Wrapper = styled.div`
  width: 100%;
  margin-bottom: ${defaultMargins.XL};
  &.fitted {
    margin-bottom: 0;
  }
`

const Row = styled.div`
  display: flex;
  align-items: center;
  color: ${colors.greyscale.medium};
  margin-bottom: ${defaultMargins.m};
  &.fitted {
    margin-bottom: 0;
  }

  h3 {
    flex-grow: 1;
  }

  &:hover {
    cursor: pointer;

    h3 {
      color: ${colors.greyscale.medium};
    }
  }
`

const IconWrapper = styled.div`
  color: ${colors.greyscale.medium};
  font-size: 28px;
  width: 35px;
  margin-right: ${defaultMargins.s};
  display: flex;
  justify-content: center;
  align-items: flex-end;
`

const ToggleWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 40px;
  width: 20px;
  font-size: 40px;
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

function CollapsibleSection({
  title,
  icon,
  children,
  startCollapsed = false,
  fitted = false,
  className,
  dataQa,
  'data-qa': dataQa2
}: CollapsibleSectionProps) {
  const [collapsed, setCollapsed] = useState<boolean>(startCollapsed)

  return (
    <Wrapper
      className={classNames(className, { fitted })}
      data-qa={dataQa2 ?? dataQa}
      data-status={collapsed ? 'closed' : 'open'}
    >
      <Row
        onClick={() => setCollapsed(!collapsed)}
        className={classNames(className, { fitted })}
      >
        {icon && (
          <IconWrapper>
            <FontAwesomeIcon icon={icon} />
          </IconWrapper>
        )}
        <H3 bold fitted>
          {title}
        </H3>
        <ToggleWrapper
          onClick={() => setCollapsed(!collapsed)}
          data-qa="collapsible-trigger"
        >
          <FontAwesomeIcon
            icon={collapsed ? faAngleUp : faAngleDown}
            color={colors.blues.primary}
          />
        </ToggleWrapper>
      </Row>
      <Content className={classNames({ collapsed })}>{children}</Content>
    </Wrapper>
  )
}

export default CollapsibleSection
