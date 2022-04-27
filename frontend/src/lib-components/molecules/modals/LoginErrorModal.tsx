// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { P } from 'lib-components/typography'
import { faTimes } from 'lib-icons'

import InfoModal from './InfoModal'

const ReturnContainer = styled.div`
  margin-top: 30px;
`

interface LoginErrorModalProps {
  translations: {
    header: string
    message: string
    returnMessage: string
  }
}

export const LoginErrorModal = React.memo(function LoginErrorModal(
  props: LoginErrorModalProps
) {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryParamName = 'loginError'
  const queryParams = searchParams

  const [errorModalVisible, setErrorModalVisible] = useState<boolean>(
    queryParams.get(queryParamName) === 'true'
  )

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
      title={props.translations.header}
      close={onClose}
      closeLabel={props.translations.returnMessage}
      type="danger"
      icon={faTimes}
    >
      <P centered>{props.translations.message}</P>
      <ReturnContainer>
        <P centered>
          <a href="#" onClick={onClose}>
            {props.translations.returnMessage}
          </a>
        </P>
      </ReturnContainer>
    </InfoModal>
  ) : null
})
