// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'

import { feeAlterationTypes } from 'lib-common/generated/api-types/invoicing'
import Select from 'lib-components/atoms/dropdowns/Select'
import InputField from 'lib-components/atoms/form/InputField'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'
import { PartialFeeAlteration } from '../../../types/fee-alteration'

interface Props {
  edited: PartialFeeAlteration
  setEdited: Dispatch<SetStateAction<PartialFeeAlteration>>
}

export default React.memo(function FeeAlterationRowInput({
  edited,
  setEdited
}: Props) {
  const { i18n } = useTranslation()
  return (
    <>
      <Select
        selectedItem={edited.type}
        items={feeAlterationTypes}
        getItemLabel={(type) => i18n.childInformation.feeAlteration.types[type]}
        onChange={(type) => type && setEdited({ ...edited, type })}
      />
      <AmountInput
        data-qa="fee-alteration-amount-input"
        type="number"
        value={edited.amount !== undefined ? edited.amount.toString() : ''}
        onChange={(value) =>
          setEdited({
            ...edited,
            amount: Math.max(0, Math.min(99999, Number(value)))
          })
        }
      />
      <IsAbsoluteRadio
        value={edited.isAbsolute}
        onChange={(isAbsolute) => setEdited({ ...edited, isAbsolute })}
      />
    </>
  )
})

const AmountInput = styled(InputField)`
  width: 5rem;
  text-align: right;
`

function IsAbsoluteRadio({
  value,
  onChange
}: {
  value: boolean
  onChange: (v: boolean) => void
}) {
  return (
    <RadioContainer>
      <RadioInput
        type="radio"
        name="is-absolute"
        id="is-absolute-false"
        value="false"
        onChange={() => onChange(false)}
      />
      <RadioLabelLeft htmlFor="is-absolute-false" selected={!value}>
        %
      </RadioLabelLeft>
      <RadioInput
        type="radio"
        name="is-absolute"
        id="is-absolute-true"
        value="true"
        onChange={() => onChange(true)}
      />
      <RadioLabelRight htmlFor="is-absolute-true" selected={value}>
        €
      </RadioLabelRight>
    </RadioContainer>
  )
}

const RadioContainer = styled.div`
  width: 140px;
  display: flex;
  justify-content: stretch;
`

const RadioInput = styled.input`
  display: none;
`

const RadioLabel = styled.label<{ selected: boolean }>`
  padding: 6px 22px;
  border: 1px solid ${colors.main.m2};
  color: ${colors.main.m2};

  ${({ selected }) => (selected ? `color: ${colors.grayscale.g0};` : '')}
  ${({ selected }) => (selected ? `background-color: ${colors.main.m2};` : '')}
`

const RadioLabelLeft = styled(RadioLabel)`
  border-right: none;
  border-radius: 2px 0 0 2px;
`

const RadioLabelRight = styled(RadioLabel)`
  border-left: none;
  border-radius: 0 2px 2px 0;
`
