import React from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'

import { useTranslations } from '../../i18n'
import ButtonContainer from '../../layout/ButtonContainer'

import BaseModal from './BaseModal'

interface Props {
  isOpen: boolean
  onClose: () => void
}

const SessionExpiredModal: React.FC<Props> = ({ isOpen, onClose }) => {
  const i18n = useTranslations()

  return (
    isOpen && (
      <BaseModal
        mobileFullScreen
        close={onClose}
        title={i18n.sessionTimeout.sessionExpiredTitle}
        closeLabel={i18n.sessionTimeout.cancel}
      >
        <p>{i18n.sessionTimeout.sessionExpiredMessage}</p>
        <ButtonContainer>
          <Button
            primary
            onClick={() => window.location.reload()}
            text={i18n.sessionTimeout.goToLoginPage}
          />
          <Button onClick={onClose} text={i18n.sessionTimeout.cancel} />
        </ButtonContainer>
      </BaseModal>
    )
  )
}

export default SessionExpiredModal
