// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import React from 'react'
import { Button, Buttons } from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'

const ButtonsContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  height: 80px;
`

interface AddButtonProps {
  submitButtonText?: string
  onCancel: () => undefined | void
  disabled?: boolean
  dataQa?: string
}
function FormActions({
  submitButtonText,
  onCancel,
  disabled = false,
  dataQa
}: AddButtonProps) {
  const { i18n } = useTranslation()
  return (
    <ButtonsContainer>
      <Buttons>
        <Button onClick={onCancel}>{i18n.common.cancel}</Button>
        <Button primary type="submit" disabled={disabled} dataQa={dataQa}>
          {submitButtonText ?? i18n.common.confirm}
        </Button>
      </Buttons>
    </ButtonsContainer>
  )
}

export default FormActions
