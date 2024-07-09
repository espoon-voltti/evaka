// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as fs from 'fs/promises'

import axios, {
  AxiosHeaders,
  AxiosResponse,
  InternalAxiosRequestConfig
} from 'axios'
import FormData from 'form-data'
import { BaseError } from 'make-error-cause'

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../config'
import { DevPerson, MockVtjDataset } from '../generated/api-types'

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

function clockHeader(now: HelsinkiDateTime): AxiosHeaders {
  return new AxiosHeaders({
    EvakaMockedTime: now.formatIso()
  })
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

type ApplicationActionSimple =
  | 'move-to-waiting-placement'
  | 'cancel-application'
  | 'set-verified'
  | 'set-unverified'
  | 'create-default-placement-plan'
  | 'confirm-placement-without-decision'
  | 'send-decisions-without-proposal'
  | 'send-placement-proposal'
  | 'confirm-decision-mailed'

export async function execSimpleApplicationAction(
  applicationId: string,
  action: ApplicationActionSimple,
  mockedTime: HelsinkiDateTime
): Promise<void> {
  try {
    await devClient.post(
      `/applications/${applicationId}/actions/${action}`,
      null,
      {
        headers: clockHeader(mockedTime)
      }
    )
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function execSimpleApplicationActions(
  applicationId: string,
  actions: ApplicationActionSimple[],
  mockedTime: HelsinkiDateTime
): Promise<void> {
  for (const action of actions) {
    await execSimpleApplicationAction(applicationId, action, mockedTime)
    await runPendingAsyncJobs(mockedTime)
  }
}

export async function runPendingAsyncJobs(
  mockedTime: HelsinkiDateTime
): Promise<void> {
  try {
    await devClient.post(`/run-jobs`, null, {
      headers: clockHeader(mockedTime)
    })
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
  pedagogicalDocumentId: string,
  employeeId: string,
  fileName: string,
  filePath: string
): Promise<string> {
  const file = await fs.readFile(`${filePath}/${fileName}`)
  const form = new FormData()
  form.append('file', file, fileName)
  form.append('employeeId', employeeId)
  try {
    return await devClient
      .post<string>(
        `/pedagogical-document-attachment/${pedagogicalDocumentId}`,
        form,
        { headers: form.getHeaders() }
      )
      .then((res) => res.data)
  } catch (e) {
    throw new DevApiError(e)
  }
}
