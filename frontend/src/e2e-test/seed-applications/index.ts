// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable no-console */

import axios from 'axios'

import { DevApiError, devClient } from '../dev-api'
import { getApplication, upsertVtjDataset } from '../generated/api-clients'

import {
  MARKER_SSN,
  buildVtjDataset,
  generateFamilies,
  seedFamiliesAndApplications,
  seedReasonings,
  seedTerms,
  seedUnits
} from './builders'
import { APPLICATION_COUNT, MARKER_APPLICATION_ID } from './config'
import { readSnapshot, writeSnapshot } from './vtj-snapshot'

interface DevCitizen {
  ssn: string | null
  firstName: string
  lastName: string
  dependantCount: number
}

async function fetchCitizens(): Promise<DevCitizen[]> {
  try {
    const { data } = await devClient.get<DevCitizen[]>('/citizen')
    return data
  } catch (e) {
    throw new Error(
      'Could not reach the dev API. Is the local dev backend running, ' +
        'and does the e2e-test config (BASE_URL / DEV_API_URL) point at it?',
      { cause: e }
    )
  }
}

// True when the seed marker is present in the in-memory VTJ mock — i.e. the
// families are already loaded in the running backend process.
async function vtjMockHasMarker(): Promise<boolean> {
  const citizens = await fetchCitizens()
  return citizens.some((c) => c.ssn === MARKER_SSN)
}

// True when the marker application exists in Postgres. Distinguishes "needs a
// full seed" from "database is seeded, only the VTJ mock needs reloading".
async function databaseHasSeed(): Promise<boolean> {
  try {
    await getApplication({ applicationId: MARKER_APPLICATION_ID })
    return true
  } catch (e) {
    if (
      e instanceof DevApiError &&
      axios.isAxiosError(e.cause) &&
      e.cause.response?.status === 404
    ) {
      return false
    }
    throw e
  }
}

// Re-registers the seeded families in the VTJ mock without touching the
// database. Prefers the snapshot written at seed time; if it is missing (e.g.
// the database was seeded before snapshots existed), regenerates the dataset
// from config — which matches the database only if config.ts is unchanged.
async function reloadVtjMock(): Promise<void> {
  let dataset = readSnapshot()
  if (dataset === null) {
    console.warn(
      'No VTJ snapshot found; regenerating from config. This matches the ' +
        'database only if config.ts is unchanged since it was seeded.'
    )
    dataset = buildVtjDataset(generateFamilies().map((g) => g.family))
    writeSnapshot(dataset)
  }
  await upsertVtjDataset({ body: dataset })
  console.log(
    `Reloaded ${dataset.persons.length} people into the VTJ mock. ` +
      'Database rows were left untouched.'
  )
}

async function fullSeed(): Promise<void> {
  console.log('Seeding preschool and club terms...')
  await seedTerms()

  console.log('Seeding decision reasonings...')
  const reasonings = await seedReasonings()

  console.log('Seeding care area and units...')
  const units = await seedUnits()

  console.log(`Seeding ~${APPLICATION_COUNT} applications...`)
  const { summary, families } = await seedFamiliesAndApplications(units)

  writeSnapshot(buildVtjDataset(families))

  console.log(
    `Done. ${summary.families} families, ${summary.applications} applications ` +
      `(DAYCARE ${summary.byKind.DAYCARE}, ` +
      `PRESCHOOL ${summary.byKind.PRESCHOOL}, ` +
      `PRESCHOOL_DAYCARE ${summary.byKind.PRESCHOOL_DAYCARE}), ` +
      `${reasonings.generic} generic + ${reasonings.individual} individual ` +
      `reasonings.`
  )
}

async function main(): Promise<void> {
  if (await vtjMockHasMarker()) {
    console.log('Already loaded in the VTJ mock — skipping.')
    return
  }

  if (await databaseHasSeed()) {
    console.log(
      'Seed data is in the database but missing from the VTJ mock ' +
        '(the backend was likely restarted). Reloading the VTJ mock only.'
    )
    await reloadVtjMock()
    return
  }

  await fullSeed()
}

main().catch((e) => {
  console.error(e instanceof Error ? e.message : e)
  process.exitCode = 1
})
