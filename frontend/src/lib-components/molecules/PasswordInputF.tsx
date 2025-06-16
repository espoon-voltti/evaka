// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { faEye, faEyeSlash } from 'lib-icons'

import { Button } from '../atoms/buttons/Button'
import type { InputFieldFProps } from '../atoms/form/InputField'
import { InputFieldF } from '../atoms/form/InputField'
import { useTranslations } from '../i18n'
import { FixedSpaceRow } from '../layout/flex-helpers'

interface PasswordInputProps extends Omit<InputFieldFProps, 'type'> {}

export default React.memo(function PasswordInputF({
  ...props
}: PasswordInputProps) {
  const [showPassword, setShowPassword] = useState(false)
  const i18n = useTranslations()

  return (
    <PasswordInputContainer spacing="xs" alignItems="start" fullWidth>
      <InputFieldF {...props} type={showPassword ? 'text' : 'password'} />
      <Button
        className="pw-toggle-button"
        appearance="inline"
        icon={showPassword ? faEyeSlash : faEye}
        text=""
        aria-label={
          showPassword ? i18n.common.hidePassword : i18n.common.showPassword
        }
        onClick={() => {
          setShowPassword(!showPassword)
        }}
      />
    </PasswordInputContainer>
  )
})

const PasswordInputContainer = styled(FixedSpaceRow)`
  height: 60px;

  .pw-toggle-button {
    width: 40px;
    margin: 10px 0 0 0;
  }
`
