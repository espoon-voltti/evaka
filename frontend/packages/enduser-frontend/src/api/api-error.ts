// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AxiosError } from 'axios'

interface RequestError {
  readonly kind: string
}

export class UnknownError implements RequestError {
  public readonly kind = 'UnknownError'
}

export class UnauthorizedError implements RequestError {
  public readonly kind = 'UnauthorizedError'
}

export const toApiError = (error: AxiosError) => {
  switch (error.response && error.response.status) {
    case 401:
      return new UnauthorizedError()
    default:
      return new UnknownError()
  }
}

export type ApiError = UnknownError | UnauthorizedError
