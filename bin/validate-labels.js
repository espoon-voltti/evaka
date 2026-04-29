#!/usr/bin/env node

// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Usage in a GitHub Actions workflow:
//
// echo '${{ toJSON(github.event.pull_request.labels[*].name) }}' | bin/check-labels.js
//

function main() {
  let inputData = ''

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
        console.error('An error occurred while parsing JSON:', error.message);
        process.exit(1);
    }
    const errors = validateLabels(json)
    if (errors.length > 0) {
      for (const error of errors) {
        console.error(error);
      }
      console.error('Other labels are ignored by the validation.')
      console.error('This pull request has the following labels:', json.join(', '));
      process.exit(1);
    }
  });
}

const changelogLabels = [
  'no-changelog',
  'breaking',
  'enhancement',
  'bug',
  'tech',
  'dependencies'
];

const scopeLabels = ['core', 'espoo', 'oulu', 'turku', 'seutu'];

const visibilityLabels = ['citizen', 'employee', 'employee-mobile', 'not-visible'];

const scopeExemptChangelogLabels = ['no-changelog', 'dependencies'];
const visibilityExemptChangelogLabels = ['no-changelog', 'dependencies', 'tech'];

function countMatches(labels, group) {
  return labels.reduce(
    (acc, label) => (group.includes(label) ? acc + 1 : acc),
    0
  );
}

function validateLabels(labels) {
  const errors = [];

  const numChangelogLabels = countMatches(labels, changelogLabels);
  if (numChangelogLabels !== 1) {
    errors.push(`Each pull request must have exactly one of the following labels: ${changelogLabels.join(', ')}`);
  }

  const numScopeLabels = countMatches(labels, scopeLabels);
  const scopeExempt = labels.some((label) => scopeExemptChangelogLabels.includes(label));
  if (!scopeExempt && numScopeLabels < 1) {
    errors.push(`Each pull request must have at least one of the following labels: ${scopeLabels.join(', ')}`);
  }

  const numVisibilityLabels = countMatches(labels, visibilityLabels);
  const visibilityExempt = labels.some((label) => visibilityExemptChangelogLabels.includes(label));
  if (!visibilityExempt && numVisibilityLabels < 1) {
    errors.push(`Each pull request must have at least one of the following labels: ${visibilityLabels.join(', ')}`);
  }
  if (labels.includes('not-visible') && numVisibilityLabels > 1) {
    errors.push(`Label 'not-visible' cannot be combined with other visibility labels: ${visibilityLabels.filter(l => l !== 'not-visible').join(', ')}`);
  }

  return errors;
}

main();
