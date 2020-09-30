// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '~state/i18n'

type LoginErrorCode = 'NO_ROLE'

export function assertErrorCode(s?: string): s is LoginErrorCode {
  return s === 'NO_ROLE'
}

interface Props {
  error?: string
}

function ErrorMessage({ error }: Props) {
  const { i18n } = useTranslation()

  if (!error) {
    return null
  }

  if (assertErrorCode(error)) {
    return <div>{i18n.login.error[error]}</div>
  }

  return <div>{i18n.login.error.default}</div>
}

export default ErrorMessage
