// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { BaseProps } from 'components/shared/utils'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { H3 } from 'components/shared/Typography'
import { faAngleDown, faAngleUp } from 'icon-set'
import colors from '@evaka/lib-components/src/colors'
import { DefaultMargins } from 'components/shared/layout/white-space'

const Wrapper = styled.div`
  width: 100%;
  margin-bottom: ${DefaultMargins.XL};
  &.fitted {
    margin-bottom: 0;
  }
`

const Row = styled.div`
  display: flex;
  align-items: baseline;
  color: ${colors.greyscale.medium};
  border-bottom: 1px solid ${colors.greyscale.lighter};
  margin-bottom: ${DefaultMargins.m};
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
  margin-right: ${DefaultMargins.s};
  display: flex;
  justify-content: center;
  align-items: flex-end;
`

const ToggleWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 30px;
  width: 30px;
  border: 1px solid ${colors.greyscale.medium};
  border-radius: 100%;
  font-size: 28px;
`

const Content = styled.div`
  &.collapsed {
    display: none;
  }
`

interface CollapsibleSectionProps extends BaseProps {
  title: string
  icon: IconDefinition
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
        <IconWrapper>
          <FontAwesomeIcon icon={icon} />
        </IconWrapper>
        <H3 fitted>{title}</H3>
        <ToggleWrapper
          onClick={() => setCollapsed(!collapsed)}
          data-qa="collapsible-trigger"
        >
          <FontAwesomeIcon icon={collapsed ? faAngleUp : faAngleDown} />
        </ToggleWrapper>
      </Row>
      <Content className={classNames({ collapsed })}>{children}</Content>
    </Wrapper>
  )
}

export default CollapsibleSection
