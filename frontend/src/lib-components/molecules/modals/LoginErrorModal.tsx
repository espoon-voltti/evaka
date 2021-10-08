// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import InfoModal from './InfoModal'
import { faTimes } from 'lib-icons'
import { useHistory, useLocation } from 'react-router-dom'
import { P } from 'lib-components/typography'
import styled from 'styled-components'

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
  const history = useHistory()
  const location = useLocation()
  const queryParamName = 'loginError'
  const queryParams = new URLSearchParams(location.search)

  const [errorModalVisible, setErrorModalVisible] = useState<boolean>(
    queryParams.get(queryParamName) === 'true'
  )

  function onClose(event?: React.UIEvent<unknown>) {
    setErrorModalVisible(false)
    queryParams.delete(queryParamName)
    history.replace({
      search: queryParams.toString()
    })
    if (event) {
      event.preventDefault()
    }
  }

  return errorModalVisible ? (
    <InfoModal
      title={props.translations.header}
      close={onClose}
      iconColour="red"
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
