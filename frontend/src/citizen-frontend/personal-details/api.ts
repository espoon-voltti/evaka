// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { PersonalDataUpdate } from 'lib-common/generated/api-types/pis'
import { client } from '../api-client'

export function updatePersonalData(
  data: PersonalDataUpdate
): Promise<Result<void>> {
  return client
    .put('/citizen/personal-data', data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
