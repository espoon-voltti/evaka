// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import Title from 'lib-components/atoms/Title'
import { Label, P } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { faLockAlt } from 'lib-icons'

import { useTranslation } from 'employee-frontend/state/i18n'
import { updatePinCode } from 'employee-frontend/api/employees'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'

export default React.memo(function EmployeePinCodePage() {
  const { i18n } = useTranslation()
  const [pin, setPin] = useState<string>('')
  const [error, setError] = useState<boolean>(false)

  function errorCheck(pin: string) {
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
    if (badPins.includes(pin)) {
      setError(true)
    } else {
      setError(false)
    }
    setPin(pin)
  }

  function savePinCode() {
    return updatePinCode(pin)
  }

  return (
    <Container>
      <Gap size={'L'} />
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

        <FixedSpaceColumn spacing={'xxs'}>
          <Label>{i18n.pinCode.pinCode}</Label>
          <InputField
            value={pin}
            onChange={errorCheck}
            placeholder={i18n.pinCode.placeholder}
            width={'s'}
            data-qa="pin-code-input"
            info={
              error
                ? {
                    text: i18n.pinCode.error,
                    status: 'warning'
                  }
                : undefined
            }
          />
        </FixedSpaceColumn>
        <Gap size={'L'} />
        {pin.length !== 4 || error ? (
          <Button primary text={i18n.pinCode.button} disabled />
        ) : (
          <AsyncButton
            primary
            text={i18n.pinCode.button}
            onClick={savePinCode}
            onSuccess={() => {
              setError(false)
            }}
          />
        )}
        <Gap size={'L'} />
      </ContentArea>
    </Container>
  )
})
