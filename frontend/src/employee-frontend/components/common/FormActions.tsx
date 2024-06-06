// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { useTranslation } from '../../state/i18n'

const ButtonsContainer = styled.div`
  display: flex;
  justify-content: flex-end;
`

interface AddButtonProps {
  submitButtonText?: string
  onCancel: () => undefined | void
  disabled?: boolean
  'data-qa'?: string
}
function FormActions({
  submitButtonText,
  onCancel,
  disabled = false,
  'data-qa': dataQa
}: AddButtonProps) {
  const { i18n } = useTranslation()
  return (
    <ButtonsContainer>
      <FixedSpaceRow>
        <Button onClick={onCancel} text={i18n.common.cancel} />
        <Button
          primary
          type="submit"
          disabled={disabled}
          data-qa={dataQa}
          text={submitButtonText ?? i18n.common.confirm}
        />
      </FixedSpaceRow>
    </ButtonsContainer>
  )
}

export default FormActions
