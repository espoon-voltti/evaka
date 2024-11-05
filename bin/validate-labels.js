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
    if (!validateLabels(json)) {
      console.error('Each pull request must have exactly one of the following labels:', knownLabels.join(', '));
      console.error('Other labels are ignored by the validation.')
      console.error('This pull request has the following labels:', json.join(', '));
      process.exit(1);
    }
  });
}

const knownLabels = [
  'no-changelog',
  'breaking',
  'enhancement',
  'bug',
  'tech',
  'dependencies'
];

function validateLabels(labels) {
  const numKnownLabels = labels.reduce(
    (acc, label) => (knownLabels.includes(label)) ? acc + 1 : acc,
    0
  );
  return numKnownLabels === 1;
}

main();
