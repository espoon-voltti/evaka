// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import UnitEditor from '~components/unit/unit-details/UnitEditor'
import { isFailure, isLoading, isSuccess, Loading, Result, Success } from '~api'
import { CareArea } from '~types/unit'
import { getAreas } from '~api/daycare'
import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import { createDaycare, DaycareFields } from '~api/unit'
import { useTranslation } from '~state/i18n'

export default function CreateUnitPage(): JSX.Element {
  const { i18n } = useTranslation()
  const [areas, setAreas] = useState<Result<CareArea[]>>(Loading)
  const [submitState, setSubmitState] = useState<Result<void> | undefined>(
    undefined
  )

  useEffect(() => {
    void getAreas().then(setAreas)
  }, [])

  const onSubmit = (fields: DaycareFields) => {
    setSubmitState(Loading)
    void createDaycare(fields).then((result) => {
      if (isSuccess(result)) {
        setSubmitState(Success(undefined))
        window.location.href = `/employee/units/${result.data}/details`
      } else {
        setSubmitState(result)
      }
    })
  }

  return (
    <Container>
      <ContentArea opaque>
        {isLoading(areas) && <Loader />}
        {isFailure(areas) && <div>{i18n.common.error.unknown}</div>}
        {isSuccess(areas) && (
          <UnitEditor
            editable={true}
            areas={areas.data}
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
