#!/usr/bin/env node

// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// oxlint-disable no-console

import { writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

const repoUrl =
  'https://github.com/passkeydeveloper/passkey-authenticator-aaguids'

const sourceUrl =
  'https://raw.githubusercontent.com/passkeydeveloper/passkey-authenticator-aaguids/main/aaguid.json'

const outputPath = fileURLToPath(
  new URL(
    '../src/citizen-frontend/auth/passkey-provider-names.ts',
    import.meta.url
  )
)

interface UpstreamEntry {
  name: string
}

const quote = (value: string) =>
  value.includes("'") ? JSON.stringify(value) : `'${value}'`

const fileContents = (entries: [string, string][]) => `\
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
// Regenerate with \`frontend/bin/generate-passkey-provider-names.ts\`

// Community-maintained list from
// ${repoUrl}
// Display-only data: unknown AAGUIDs fall back to a generic label.
export const passkeyProviderNamesByAaguid: Partial<Record<string, string>> = {
${entries.map(([aaguid, name]) => `  ${quote(aaguid)}: ${quote(name)}`).join(',\n')}
}
`

async function main() {
  const response = await fetch(sourceUrl)
  if (!response.ok) {
    throw new Error(`${sourceUrl} returned HTTP ${response.status}`)
  }
  const upstream = (await response.json()) as Record<string, UpstreamEntry>
  // Sorted by AAGUID so that regeneration only diffs genuinely changed entries
  const entries = Object.entries(upstream)
    .map(([aaguid, { name }]): [string, string] => [aaguid, name])
    .sort(([a], [b]) => a.localeCompare(b))
  await writeFile(outputPath, fileContents(entries))
  console.log(`Wrote ${entries.length} providers to ${outputPath}`)
}

await main()
