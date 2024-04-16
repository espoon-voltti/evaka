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
      const grouped = processPrs(json, startDate, endDate)
      const markdown = toMarkdown(grouped, startDate, endDate)
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

const ignoreLabels = ['no-changelog']

const labels = {
  breaking: 'Rikkovat muutokset',
  enhancement: 'Uudet ominaisuudet ja parannukset',
  bug: 'Bugikorjaukset',
  unknown: 'Muut',
  tech: 'Tekniset',
  dependencies: 'Riippuvuuksien päivitykset',
}

function processPrs(prs, startDate, endDate) {
  const min = startDate
  const maxExclusive = new Date(endDate + 24 * 60 * 60 * 1000)

  const prsToInclude = prs
    .filter((pr) => min <= new Date(pr.closedAt) && new Date(pr.closedAt) < maxExclusive)
    .filter((pr) => !ignoreLabels.some((ignore) => hasLabel(pr, ignore)))

  // Sort by closedAt descending
  prsToInclude.sort((a, b) => a.closedAt < b.closedAt ? 1 : -1);

  const grouped = groupBy(prsToInclude, (pr) => {
    for (const label of Object.keys(labels)) {
      if (hasLabel(pr, label)) return label
    }
    return 'unknown'
  })

  return grouped
}

function toMarkdown(groups, startDate, endDate) {
  let markdown = [`# eVakan muutosloki ${formatDate(startDate)}-${formatDate(endDate)}`]
  for (const [label, title] of Object.entries(labels)) {
    const prs = groups[label]
    if (!prs || !prs.length) continue

    const lines = prs.map((pr) => {
      return `- ${pr.title} [#${pr.number}](${pr.url})`
    })
    markdown.push('', `## ${title}`, '', ...lines)
  }
  return markdown.join('\n')
}

function groupBy(arr, fn) {
  const result = {}
  arr.forEach((item) => {
    const key = fn(item)
    if (result[key] === undefined) {
      result[key] = []
    }
    result[key].push(item)
  })
  return result
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
