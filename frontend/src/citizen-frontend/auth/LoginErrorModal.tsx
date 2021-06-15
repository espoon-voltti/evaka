// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, Fragment } from 'react'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faTimes } from 'lib-icons'
import { useHistory } from 'react-router-dom'
import { useTranslation } from '../localization'
import { P } from 'lib-components/typography'
import styled from 'styled-components'

const ReturnContainer = styled.div`
  margin-top: 30px;
`

export const LoginErrorModal = React.memo(function LoginErrorModal() {
  const t = useTranslation()

  const queryParams = new URLSearchParams(location.search)
  const [errorModalVisible, setErrorModalVisible] = useState<boolean>(
    queryParams.get('loginError') === 'true'
  )
  const history = useHistory()

  function closeErrorModal(event?: React.UIEvent<unknown>) {
    setErrorModalVisible(false)
    queryParams.delete('loginError')
    history.replace({
      search: queryParams.toString()
    })
    if (event) {
      event.preventDefault()
    }
  }

  return (
    <>
      {errorModalVisible && (
        <InfoModal
          title={t.login.failedModal.header}
          close={closeErrorModal}
          iconColour="red"
          icon={faTimes}
        >
          <Fragment>
            <P centered>{t.login.failedModal.message}</P>
            <ReturnContainer>
              <P centered>
                <a href="#" onClick={closeErrorModal}>
                  {t.login.failedModal.returnMessage}
                </a>
              </P>
            </ReturnContainer>
          </Fragment>
        </InfoModal>
      )}
    </>
  )
})
