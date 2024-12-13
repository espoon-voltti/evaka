// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Button } from 'lib-components/atoms/buttons/Button'

import { useTranslations } from '../../i18n'
import { defaultMargins } from '../../white-space'

import BaseModal from './BaseModal'

interface Props {
  onClose: () => void
  onLoginClick?: () => void
}

const SessionExpiredModal: React.FC<Props> = ({
  onLoginClick = () => window.location.reload(),
  onClose
}) => {
  const i18n = useTranslations()

  return (
    <BaseModal
      mobileFullScreen
      close={onClose}
      title={i18n.sessionTimeout.sessionExpiredTitle}
      closeLabel={i18n.sessionTimeout.cancel}
      zIndex={999}
    >
      <CenteredP>{i18n.sessionTimeout.sessionExpiredMessage}</CenteredP>
      <ButtonFooter>
        <Button
          primary
          onClick={onLoginClick}
          text={i18n.sessionTimeout.goToLoginPage}
        />
        <Button onClick={onClose} text={i18n.sessionTimeout.cancel} />
      </ButtonFooter>
    </BaseModal>
  )
}
const CenteredP = styled.p`
  text-align: center;
`
export default SessionExpiredModal
const ButtonFooter = styled.div`
  display: flex;
  justify-content: space-between;
  padding: 0 0 ${defaultMargins.L} 0;
`
