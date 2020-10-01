// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { memo } from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleDown } from '@evaka/icons'
import {
  Select,
  SelectProps,
  SelectOptionProps
} from '~components/shared/alpha'
import { EspooColours } from 'utils/colours'

interface ContainerProps {
  fullWidth?: boolean
}

const Container = styled.div<ContainerProps>`
  position: relative;
  ${(p) => (p.fullWidth ? 'width: 100%;' : '')}

  select {
    padding-right: 20px;
  }
`

const Icon = styled(FontAwesomeIcon)`
  pointer-events: none;
  position: absolute;
  right: 4px;
  top: 0px;
  height: 100%;
`

type SelectWithIconProps = SelectProps & ContainerProps

const SelectWithIcon = memo(function SelectWithArrow({
  fullWidth,
  ...props
}: SelectWithIconProps) {
  return (
    <Container fullWidth={fullWidth}>
      <Select {...props} />
      <Icon icon={faAngleDown} size={'lg'} color={EspooColours.greyDark} />
    </Container>
  )
})

export default SelectWithIcon
export { SelectProps, SelectOptionProps }
