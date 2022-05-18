// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from '../../../state/i18n'

interface Props {
  error?: string
}

function ErrorMessage({ error }: Props) {
  const { i18n } = useTranslation()

  if (!error) {
    return null
  }

  if (error === 'NO_ROLE') {
    return <div>{i18n.login.error.noRole}</div>
  }

  return <div>{i18n.login.error.default}</div>
}

export default ErrorMessage
