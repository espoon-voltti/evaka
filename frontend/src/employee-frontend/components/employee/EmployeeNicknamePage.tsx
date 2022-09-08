// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { Loading, Result } from 'lib-common/api'
import { EmployeeNicknames } from 'lib-common/generated/api-types/pis'
import Title from 'lib-components/atoms/Title'
import Select from 'lib-components/atoms/dropdowns/Select'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'

import { getPossibleNicknames } from '../../api/employees'
import { useTranslation } from '../../state/i18n'

export default React.memo(function EmployeeNicknamePage() {
  const { i18n } = useTranslation()
  const [possibleNicknames, setPossibleNicknames] = useState<
    Result<EmployeeNicknames>
  >(Loading.of())
  const [selectedNickname, setSelectedNickname] = useState<string | null>(null)

  useEffect(() => {
    setPossibleNicknames(Loading.of())
    void getPossibleNicknames().then(setPossibleNicknames)
  }, [])

  return (
    <Container>
      <ContentArea opaque>
        <Title>{i18n.nickname.popupLink}</Title>
        <P>{i18n.nickname.description}</P>
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.nickname.select}</Label>
          <Select
            items={
              possibleNicknames.isSuccess
                ? possibleNicknames.value.possibleNicknames
                : []
            }
            selectedItem={selectedNickname}
            onChange={(value) => setSelectedNickname(value)}
            data-qa="select-nickname"
          />
        </FixedSpaceColumn>
      </ContentArea>
    </Container>
  )
})
