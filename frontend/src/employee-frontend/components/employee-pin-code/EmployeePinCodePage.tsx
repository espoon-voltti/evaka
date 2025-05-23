// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useState } from 'react'

import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import type { InputInfo } from 'lib-components/atoms/form/InputField'
import InputField from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faLockAlt } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { useWarnOnUnsavedChanges } from '../../utils/useWarnOnUnsavedChanges'

import { isPinLockedQuery, upsertPinCodeMutation } from './queries'
import { isValidPinCode } from './utils'

export default React.memo(function EmployeePinCodePage() {
  const { i18n } = useTranslation()
  const [pin, setPin] = useState<string>('')
  const [error, setError] = useState<boolean>(false)
  const pinLocked = useQueryResult(isPinLockedQuery()).getOrElse(false)
  const [dirty, setDirty] = useState<boolean>(false)

  function errorCheck(pin: string) {
    if (!isValidPinCode(pin)) {
      setError(true)
    } else {
      setError(false)
    }
    setPin(pin)
    setDirty(pin.length > 0)
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
  // TODO: hard to achieve with wouter
  // usePrompt({ message: i18n.pinCode.unsavedDataWarning, when: dirty })

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
        <MutateButton
          primary
          text={i18n.pinCode.button}
          disabled={pin.length !== 4 || error}
          mutation={upsertPinCodeMutation}
          onClick={() => ({
            body: {
              pin
            }
          })}
          onSuccess={() => {
            setDirty(false)
            setError(false)
          }}
          data-qa="send-pin-button"
        />
        <Gap size="L" />
      </ContentArea>
    </Container>
  )
})
