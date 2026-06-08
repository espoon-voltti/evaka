// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import type { MockVtjDataset } from '../generated/api-types'

// The seeded families' VTJ dataset, written next to this tool at seed time. The
// mock VTJ store is in-memory in the backend process, so a restart wipes it
// while the database rows survive; replaying this file re-registers the
// families without recreating those rows. It is paired with the seeded database
// state — a full reseed overwrites it, a database wipe invalidates it. The
// seed-applications folder is git-excluded, so this file stays local.
const SNAPSHOT_PATH = join(
  dirname(fileURLToPath(import.meta.url)),
  '.vtj-snapshot.json'
)

export function readSnapshot(): MockVtjDataset | null {
  if (!existsSync(SNAPSHOT_PATH)) return null
  return JSON.parse(readFileSync(SNAPSHOT_PATH, 'utf-8')) as MockVtjDataset
}

export function writeSnapshot(dataset: MockVtjDataset): void {
  writeFileSync(SNAPSHOT_PATH, JSON.stringify(dataset, null, 2))
}
