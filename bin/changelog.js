#!/usr/bin/env node

// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Usage:
//
// gh pr -R espoon-voltti/evaka list \
//     --base master \
//     --state merged \
//     --limit 100 \
//     --json title,labels,closedAt,number,url \
//     | bin/changelog.js <START-DATE> <END-DATE>
//
// Give the dates in YYYY-MM-DD format. The end date is inclusive.
//

function main() {
  let inputData = ''

  const { startDate, endDate } = parseArgs()

  const stdin = process.stdin;
  stdin.setEncoding('utf-8')
  stdin.on('data', (data) => {
      inputData += data
  });

  stdin.on('end', () => {
      let json
      try {
          json = JSON.parse(inputData);
      } catch (error) {
          console.error("An error occurred while parsing JSON:", error.message);
      }
      const prs = processPrs(json, startDate, endDate)
      const markdown = toMarkdown(prs, startDate, endDate)
      console.log(markdown)
  });
}

function parseArgs() {
  const args = process.argv.slice(2)

  if (args.length != 2) {
      console.log("Please provide two date arguments in YYYY-MM-DD format")
      process.exit(1)
  }

  const startDate = new Date(args[0])
  const endDate = new Date(args[1])

  if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
      console.log("Please provide two date arguments in YYYY-MM-DD format")
      process.exit(1)
  }

  return {startDate, endDate}
}

const scopeSections = [
  { label: 'core', title: 'Ytimen muutokset' },
  { label: 'espoo', title: 'Espoo' },
  { label: 'oulu', title: 'Oulu' },
  { label: 'turku', title: 'Turku' },
  { label: 'seutu', title: 'Seutu' },
]

const changelogSections = {
  breaking: 'Toimia vaativat muutokset',
  enhancement:  'Uudet ominaisuudet ja parannukset',
  bugfix: 'Bugikorjaukset',
  tech: 'Tekniset',
  dependencies: 'Riippuvuuksien päivitykset',
  unknown: 'Muut',
}

const visibilityLabels = {
  citizen: 'kuntalainen',
  employee: 'henkilökunta',
  'employee-mobile': 'henkilökunnan mobiili',
}

function processPrs(prs, startDate, endDate) {
  const min = startDate.getTime()
  const maxExclusive = endDate.getTime() + 24 * 60 * 60 * 1000

  return prs
    .filter((pr) => {
      const t = new Date(pr.closedAt).getTime()
      return min <= t && t < maxExclusive
    })
    .filter((pr) => !hasLabel(pr, 'no-changelog'))
    .sort((a, b) => a.closedAt < b.closedAt ? 1 : -1)
}

function toMarkdown(prs, startDate, endDate) {
  const parts = [`# eVakan muutosloki ${formatDate(startDate)}-${formatDate(endDate)}`]

  for (const { label: scopeLabel, title: scopeTitle } of scopeSections) {
    const hasNoScopeLabel = (pr) => !scopeSections.some(({ label }) => hasLabel(pr, label))
    const scopePrs = prs.filter((pr) => hasLabel(pr, scopeLabel) || (scopeLabel === 'core' && hasNoScopeLabel(pr)))
    if (scopePrs.length === 0) continue

    parts.push('', `## ${scopeTitle}`)

    for (const [changelogLabel, changelogTitle] of Object.entries(changelogSections)) {
      const sectionPrs = scopePrs.filter((pr) => changelogLabel === 'unknown'
        ? !Object.keys(changelogSections).filter((l) => l !== 'unknown').some((l) => hasLabel(pr, l))
        : hasLabel(pr, changelogLabel)
      )
      if (sectionPrs.length === 0) continue

      const lines = sectionPrs.map(formatPrLine)
      if (changelogLabel === 'dependencies') {
        parts.push('', `<details><summary>${changelogTitle}</summary>`, '', ...lines, '', '</details>')
      } else {
        parts.push('', `### ${changelogTitle}`, '', ...lines)
      }
    }
  }

  return parts.join('\n')
}

function formatPrLine(pr) {
  const visibility = Object.entries(visibilityLabels)
    .filter(([label]) => hasLabel(pr, label))
    .map(([, name]) => name)

  const suffix = visibility.length > 0 ? ` _(${visibility.join(', ')})_` : ''
  return `- ${pr.title} [#${pr.number}](${pr.url})${suffix}`
}

function hasLabel(pr, labelName) {
  for (const label of pr.labels) {
    if (label.name === labelName) return true
  }
  return false
}

function formatDate(date) {
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()

  return `${day}.${month}.${year}`
}

main()
