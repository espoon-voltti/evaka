// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'

import type { Download } from 'playwright'

import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { JsonOf } from 'lib-common/json'
import type {
  CitizenCustomizations,
  CommonCustomizations,
  DeepPartial,
  EmployeeCustomizations,
  EmployeeMobileCustomizations
} from 'lib-customizations/types'

declare global {
  var evaka:
    | {
        captureScreenshots: (namePrefix: string) => Promise<void>
        saveTraces: (namePrefix: string) => Promise<void>
        promises: Promise<void>[]
        keepSessionAliveThrottleTime?: number
      }
    | undefined
}

export interface EvakaBrowserContextOptions {
  mockedTime?: HelsinkiDateTime
  citizenCustomizations?: DeepPartial<JsonOf<CitizenCustomizations>>
  employeeCustomizations?: DeepPartial<JsonOf<EmployeeCustomizations>>
  employeeMobileCustomizations?: DeepPartial<
    JsonOf<EmployeeMobileCustomizations>
  >
  commonCustomizations?: DeepPartial<JsonOf<CommonCustomizations>>
}

export async function captureTextualDownload(
  download: Download
): Promise<string> {
  const filePath = await download.path()
  if (!filePath) throw new Error('Download failed')
  return new Promise<string>((resolve, reject) =>
    fs.readFile(filePath, 'utf-8', (err, data) =>
      err ? reject(err) : resolve(data)
    )
  )
}

export const mobileViewport = { width: 360, height: 740 }
