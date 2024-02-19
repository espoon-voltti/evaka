// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { P } from 'lib-components/typography'
import { faTimes } from 'lib-icons'

import { useTranslations } from '../../i18n'

import InfoModal from './InfoModal'

const ReturnContainer = styled.div`
  margin-top: 30px;
`

export const LoginErrorModal = React.memo(function LoginErrorModal() {
  const i18n = useTranslations()
  const [searchParams, setSearchParams] = useSearchParams()
  const queryParamName = 'loginError'
  const queryParams = searchParams

  const [errorModalVisible, setErrorModalVisible] = useState<boolean>(
    queryParams.get(queryParamName) === 'true'
  )
  const [errorCode] = useState(queryParams.get('errorCode'))

  function onClose(event?: React.UIEvent<unknown>) {
    setErrorModalVisible(false)
    queryParams.delete(queryParamName)
    setSearchParams(queryParams, { replace: true })
    if (event) {
      event.preventDefault()
    }
  }

  return errorModalVisible ? (
    <InfoModal
      title={i18n.loginErrorModal.header}
      close={onClose}
      closeLabel={i18n.loginErrorModal.returnMessage}
      type="danger"
      icon={faTimes}
    >
      <P centered>
        {errorCode === 'inactiveUser'
          ? i18n.loginErrorModal.inactiveUserMessage
          : i18n.loginErrorModal.message}
      </P>
      <ReturnContainer>
        <P centered>
          <a href="#" onClick={onClose}>
            {i18n.loginErrorModal.returnMessage}
          </a>
        </P>
      </ReturnContainer>
    </InfoModal>
  ) : null
})
