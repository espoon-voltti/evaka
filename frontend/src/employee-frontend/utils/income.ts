// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useState } from 'react'

import {
  getIncomeOptions,
  IncomeTypeOptions
} from 'employee-frontend/api/income'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'

export function useIncomeTypeOptions() {
  const [incomeTypeOptions, setIncomeTypeOptions] = useState<
    Result<IncomeTypeOptions>
  >(Loading.of())

  const loadIncomeTypeOptions = useRestApi(
    getIncomeOptions,
    setIncomeTypeOptions
  )
  useEffect(() => {
    loadIncomeTypeOptions()
  }, [loadIncomeTypeOptions])

  return incomeTypeOptions
}
