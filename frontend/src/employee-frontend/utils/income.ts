// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useState } from 'react'

import { Loading, Result, wrapResult } from 'lib-common/api'
import { IncomeTypeOptions } from 'lib-common/generated/api-types/invoicing'
import { useRestApi } from 'lib-common/utils/useRestApi'

import { getIncomeTypeOptions } from '../generated/api-clients/invoicing'

const getIncomeTypeOptionsResult = wrapResult(getIncomeTypeOptions)

export function useIncomeTypeOptions() {
  const [incomeTypeOptions, setIncomeTypeOptions] = useState<
    Result<IncomeTypeOptions>
  >(Loading.of())

  const loadIncomeTypeOptions = useRestApi(
    getIncomeTypeOptionsResult,
    setIncomeTypeOptions
  )
  useEffect(() => {
    void loadIncomeTypeOptions()
  }, [loadIncomeTypeOptions])

  return incomeTypeOptions
}
