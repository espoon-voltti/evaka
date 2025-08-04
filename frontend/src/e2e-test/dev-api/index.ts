// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as fs from 'fs/promises'

import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import axios from 'axios'
import { BaseError } from 'make-error-cause'

import type { SimpleApplicationAction } from 'lib-common/generated/api-types/application'
import type {
  ApplicationId,
  EmployeeId,
  PedagogicalDocumentId
} from 'lib-common/generated/api-types/shared'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../config'
import {
  createDefaultPlacementPlan,
  createPedagogicalDocumentAttachment,
  runJobs,
  simpleAction
} from '../generated/api-clients'
import type { DevPerson, MockVtjDataset } from '../generated/api-types'

export class DevApiError extends BaseError {
  constructor(cause: unknown) {
    if (axios.isAxiosError(cause)) {
      const message = `${cause.message}: ${formatRequest(
        cause.config
      )}\n${formatResponse(cause.response)}`
      super(message, cause)
    } else if (cause instanceof Error) {
      super('Dev API error', cause)
    } else {
      super('Dev API error')
    }
  }
}

function formatRequest(config: InternalAxiosRequestConfig | undefined): string {
  if (!config || !config.url) return ''
  const url = new URL(config.url, config.baseURL)
  return `${String(config.method)} ${url.pathname}${url.search}${url.hash}`
}

function formatResponse(response: AxiosResponse | undefined): string {
  if (!response) return '[no response]'
  if (!response.data) return '[no response body]'
  if (typeof response.data === 'string') return response.data
  return JSON.stringify(response.data, null, 2)
}

export const devClient = axios.create({
  baseURL: config.devApiGwUrl
})

type DevApplicationAction =
  | SimpleApplicationAction
  | 'CREATE_DEFAULT_PLACEMENT_PLAN'

export async function execSimpleApplicationAction(
  applicationId: ApplicationId,
  action: DevApplicationAction,
  mockedTime: HelsinkiDateTime
): Promise<void> {
  try {
    if (action === 'CREATE_DEFAULT_PLACEMENT_PLAN') {
      await createDefaultPlacementPlan({ applicationId }, { mockedTime })
    } else {
      await simpleAction({ applicationId, action }, { mockedTime })
    }
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function execSimpleApplicationActions(
  applicationId: ApplicationId,
  actions: DevApplicationAction[],
  mockedTime: HelsinkiDateTime
): Promise<void> {
  for (const action of actions) {
    await execSimpleApplicationAction(applicationId, action, mockedTime)
  }
}

export async function runPendingAsyncJobs(
  mockedTime: HelsinkiDateTime
): Promise<void> {
  try {
    await runJobs({ mockedTime })
  } catch (e) {
    throw new DevApiError(e)
  }
}

export const vtjDependants = (
  guardian: DevPerson,
  ...dependants: DevPerson[]
): MockVtjDataset => {
  const guardianSsn = guardian.ssn
  if (!guardianSsn) throw new Error('Guardian must have SSN')
  return {
    persons: [],
    guardianDependants: {
      [guardianSsn]: dependants.map((d) => {
        const ssn = d.ssn
        if (!ssn) throw new Error('Dependant must have SSN')
        return ssn
      })
    }
  }
}

export async function insertPedagogicalDocumentAttachment(
  pedagogicalDocumentId: PedagogicalDocumentId,
  employeeId: EmployeeId,
  fileName: string,
  filePath: string
): Promise<string> {
  const file = await fs.readFile(`${filePath}/${fileName}`)
  return await createPedagogicalDocumentAttachment({
    pedagogicalDocumentId,
    file: new File([new Uint8Array(file)], fileName),
    employeeId
  })
}
