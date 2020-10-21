// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import UnitEditor from '~components/unit/unit-details/UnitEditor'
import { isFailure, isLoading, isSuccess, Loading, Result, Success } from '~api'
import { CareArea } from '~types/unit'
import { getAreas } from '~api/daycare'
import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import { Gap } from '~components/shared/layout/white-space'
import { useParams } from 'react-router-dom'
import {
  DaycareFields,
  getDaycare,
  UnitResponse,
  updateDaycare
} from '~api/unit'
import { TitleContext, TitleState } from '~state/title'
import { useTranslation } from '~state/i18n'

export default function UnitDetailsPage(): JSX.Element {
  const { id } = useParams<{ id: string }>()
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading)
  const [areas, setAreas] = useState<Result<CareArea[]>>(Loading)
  const [editable, setEditable] = useState(false)
  const [submitState, setSubmitState] = useState<Result<void> | undefined>(
    undefined
  )
  useEffect(() => {
    if (isSuccess(unit)) {
      setTitle(unit.data.daycare.name)
    }
  }, [unit])

  useEffect(() => {
    void getAreas().then(setAreas)
    void getDaycare(id).then(setUnit)
  }, [])

  const onSubmit = (fields: DaycareFields, currentUnit: UnitResponse) => {
    if (!id) return
    setSubmitState(Loading)
    void updateDaycare(id, fields).then((result) => {
      if (isSuccess(result)) {
        setUnit(Success({ ...currentUnit, daycare: result.data }))
        setSubmitState(Success(undefined))
        setEditable(false)
      } else {
        setSubmitState(result)
      }
    })
  }

  const loading = isLoading(areas) || isLoading(unit)
  const failure = isFailure(areas) || isFailure(unit)

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        {loading && <Loader />}
        {!loading && failure && <div>{i18n.common.error.unknown}</div>}
        {isSuccess(areas) && isSuccess(unit) && (
          <UnitEditor
            editable={editable}
            areas={areas.data}
            unit={unit.data.daycare}
            submit={submitState}
            onSubmit={(fields) => onSubmit(fields, unit.data)}
            onClickCancel={() => setEditable(false)}
            onClickEdit={() => setEditable(true)}
          />
        )}
      </ContentArea>
    </Container>
  )
}
