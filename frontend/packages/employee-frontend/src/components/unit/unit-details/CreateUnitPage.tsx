// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useHistory } from 'react-router-dom'
import UnitEditor from '~components/unit/unit-details/UnitEditor'
import { Loading, Result } from '~api'
import { CareArea } from '~types/unit'
import { getAreas } from '~api/daycare'
import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import { createDaycare, DaycareFields } from '~api/unit'
import { useTranslation } from '~state/i18n'

export default function CreateUnitPage(): JSX.Element {
  const history = useHistory()
  const { i18n } = useTranslation()
  const [areas, setAreas] = useState<Result<CareArea[]>>(Loading.of())
  const [submitState, setSubmitState] = useState<Result<void> | undefined>(
    undefined
  )

  useEffect(() => {
    void getAreas().then(setAreas)
  }, [])

  const onSubmit = (fields: DaycareFields) => {
    setSubmitState(Loading.of())
    void createDaycare(fields).then((result) => {
      setSubmitState(result.map(() => undefined))
      if (result.isSuccess) {
        history.push(`/employee/units/${result.value}/details`)
      }
    })
  }

  return (
    <Container>
      <ContentArea opaque>
        {areas.isLoading && <Loader />}
        {areas.isFailure && <div>{i18n.common.error.unknown}</div>}
        {areas.isSuccess && (
          <UnitEditor
            editable={true}
            areas={areas.value}
            unit={undefined}
            submit={submitState}
            onSubmit={onSubmit}
            onClickCancel={() => window.history.back()}
          />
        )}
      </ContentArea>
    </Container>
  )
}
