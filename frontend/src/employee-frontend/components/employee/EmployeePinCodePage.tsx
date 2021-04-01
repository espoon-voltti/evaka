// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import Title from 'lib-components/atoms/Title'
import { Label, P } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Button from 'lib-components/atoms/buttons/Button'

export default React.memo(function EmployeePinCodePage() {
  const [pin, setPin] = useState<string>('')

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <Title>eVaka-mobiilin PIN-koodi</Title>
        <P>
          Tällä sivulla voit asettaa oman henkilökohtaisen PIN-koodisi Espoon
          varhaiskasvatuksen mobiilisovellusta varten. PIN-koodia käytetään
          eVaka-mobiilissa lukon
        </P>
        <P>
          <strong>Huom!</strong> Ethän luovuta PIN-koodiasi kenenkään toisen
          henkilön tietoon. Tarvittaessa voit vaihtaa PIN-koodin milloin vain.
        </P>
        <Title size={2}>Aseta PIN-koodi</Title>
        <P>
          PIN-koodin tulee sisältää neljä (4) numeroa. Yleisimmät
          numeroyhdistelmät (esim. 1234) eivät kelpaa.
        </P>

        <FixedSpaceColumn spacing={'xxs'}>
          <Label>PIN-koodi</Label>
          <InputField
            value={pin}
            onChange={setPin}
            placeholder={`4 numeroa`}
            width={'s'}
            data-qa="pin-code-input"
          />
        </FixedSpaceColumn>
        <Gap size={'L'} />
        <Button primary text="Tallenna PIN-koodi" />
        <Gap size={'L'} />
      </ContentArea>
    </Container>
  )
})
