// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Multiselect } from 'multiselect-react-dropdown'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleDown } from '@evaka/icons'
import Colors from '~components/shared/Colors'

interface MultiSelectProps {
  options: SelectOptionProp[]
  onSelect: (option: SelectOptionProp[]) => void
  onRemove: (option: SelectOptionProp[]) => void
  placeholder?: string
  'data-qa'?: string
}

const MultiSelectContainer = styled.div`
  #multiselectContainerReact {
    cursor: pointer;

    div:first-child {
      border-top: 1px solid transparent;
      border-left: 1px solid transparent;
      border-right: 1px solid transparent;
      border-bottom: 1px solid ${Colors.greyscale.medium};
      border-radius: 0px;

      &:focus-within {
        border-bottom: 2px solid ${Colors.accents.petrol};
        margin-bottom: -1px;
      }

      span {
        font-size: 14px;
        border-radius: 2px;
        background: ${Colors.blues.primary};
      }

      input {
        font-size: 15px;
        color: ${Colors.greyscale.dark};
        font-style: italic;
      }
    }

    .optionListContainer {
      .highlight {
        background: ${Colors.blues.primary};
      }

      li:hover {
        background: ${Colors.blues.primary};
      }
    }
  }
`

function MultiSelect({
  options,
  placeholder,
  onSelect,
  onRemove,
  'data-qa': dataQa
}: MultiSelectProps) {
  return (
    <MultiSelectContainer data-qa={dataQa}>
      <Multiselect
        options={options} // Options to display in the dropdown
        onSelect={onSelect} // Function will trigger on select event
        onRemove={onRemove} // Function will trigger on remove event
        displayValue="label" // Property name to display in the dropdown options
        closeIcon="cancel"
        placeholder={placeholder}
      />
      <Icon icon={faAngleDown} size={'lg'} color={Colors.greyscale.dark} />
    </MultiSelectContainer>
  )
}

const Icon = styled(FontAwesomeIcon)`
  position: relative;
  right: 8px;
  float: right;
  top: -26px;
`

export interface SelectOptionProp {
  id: string
  label: string
}

export default MultiSelect
