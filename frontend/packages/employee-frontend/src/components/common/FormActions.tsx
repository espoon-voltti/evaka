// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import React from 'react'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { useTranslation } from '~state/i18n'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'

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
      <FixedSpaceRow>
        <Button onClick={onCancel} text={i18n.common.cancel} />
        <Button
          primary
          type="submit"
          disabled={disabled}
          dataQa={dataQa}
          text={submitButtonText ?? i18n.common.confirm}
        />
      </FixedSpaceRow>
    </ButtonsContainer>
  )
}

export default FormActions
