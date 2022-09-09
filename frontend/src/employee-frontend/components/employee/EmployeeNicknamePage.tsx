// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { Loading, Result } from 'lib-common/api'
import { EmployeeNicknames } from 'lib-common/generated/api-types/pis'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'

import { getPossibleNicknames, setEmployeeNickname } from '../../api/employees'
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

  useEffect(() => {
    if (possibleNicknames.isSuccess) {
      setSelectedNickname(possibleNicknames.value.selectedNickname)
    }
  }, [possibleNicknames])

  const onSave = () => {
    return setEmployeeNickname({
      nickname: selectedNickname
    })
  }

  const disableConfirm = () => {
    return possibleNicknames.isSuccess
      ? possibleNicknames.value.selectedNickname != null &&
          possibleNicknames.value.selectedNickname == selectedNickname
      : false
  }

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
            selectedItem={
              possibleNicknames.isSuccess
                ? selectedNickname || possibleNicknames.value.selectedNickname
                : null
            }
            onChange={(value) => setSelectedNickname(value)}
            data-qa="select-nickname"
          />
          <AsyncButton
            primary
            disabled={disableConfirm()}
            text={i18n.nickname.confirm}
            onClick={onSave}
            onSuccess={() => location.reload()}
            data-qa="confirm-button"
          />
        </FixedSpaceColumn>
      </ContentArea>
    </Container>
  )
})
