// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect, useState } from 'react'
import { unstable_usePrompt as usePrompt } from 'react-router-dom'

import { wrapResult } from 'lib-common/api'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import InputField, { InputInfo } from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faLockAlt } from 'lib-icons'

import { isPinLocked, upsertPinCode } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { useWarnOnUnsavedChanges } from '../../utils/useWarnOnUnsavedChanges'

const isPinLockedResult = wrapResult(isPinLocked)
const upsertPinCodeResult = wrapResult(upsertPinCode)

const badPins = [
  '1234',
  '0000',
  '1111',
  '2222',
  '3333',
  '4444',
  '5555',
  '6666',
  '7777',
  '8888',
  '9999'
]

export function isValidPinCode(pin: string) {
  return !badPins.includes(pin) && /^\d{4}$/.test(pin)
}

export default React.memo(function EmployeePinCodePage() {
  const { i18n } = useTranslation()
  const [pin, setPin] = useState<string>('')
  const [error, setError] = useState<boolean>(false)
  const [pinLocked, setPinLocked] = useState<boolean>(false)
  const [dirty, setDirty] = useState<boolean>(false)

  useEffect(() => {
    void isPinLockedResult().then((res) => res.map(setPinLocked))
  }, [setPinLocked])

  function errorCheck(pin: string) {
    if (!isValidPinCode(pin)) {
      setError(true)
    } else {
      setError(false)
    }
    setPin(pin)
    setDirty(pin.length > 0)
  }

  function savePinCode() {
    return upsertPinCodeResult({ body: { pin } })
      .then(() => setDirty(false))
      .then(isPinLockedResult)
  }

  function getInputInfo(): InputInfo | undefined {
    return pin && error
      ? {
          text: i18n.pinCode.error,
          status: 'warning'
        }
      : pinLocked && !pin
        ? {
            text: i18n.pinCode.locked,
            status: 'warning'
          }
        : undefined
  }

  useWarnOnUnsavedChanges(dirty, i18n.pinCode.unsavedDataWarning)
  usePrompt({ message: i18n.pinCode.unsavedDataWarning, when: dirty })

  return (
    <Container>
      <ContentArea opaque>
        <Title>{i18n.pinCode.title}</Title>
        <P>
          {i18n.pinCode.text1} <FontAwesomeIcon icon={faLockAlt} />{' '}
          {i18n.pinCode.text2}
        </P>
        <P>
          <strong>{i18n.pinCode.text3}</strong> {i18n.pinCode.text4}
        </P>
        <Title size={2}>{i18n.pinCode.title2}</Title>
        <P>{i18n.pinCode.text5}</P>

        {pinLocked && (
          <AlertBox
            data-qa="pin-locked-alert-box"
            message={i18n.pinCode.lockedLong}
          />
        )}

        <FixedSpaceColumn spacing="xxs">
          <Label>{i18n.pinCode.pinCode}</Label>
          <InputField
            value={pin}
            onChange={errorCheck}
            placeholder={i18n.pinCode.placeholder}
            width="s"
            data-qa="pin-code-input"
            info={getInputInfo()}
          />
        </FixedSpaceColumn>
        <Gap size="L" />
        {pin.length !== 4 || error ? (
          <Button primary text={i18n.pinCode.button} disabled />
        ) : (
          <AsyncButton
            primary
            text={i18n.pinCode.button}
            onClick={savePinCode}
            onSuccess={(isLocked) => {
              setPinLocked(isLocked)
              setError(false)
            }}
            data-qa="send-pin-button"
          />
        )}
        <Gap size="L" />
      </ContentArea>
    </Container>
  )
})
