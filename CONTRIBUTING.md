<!--
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Contributing

We appreciate your interest in contributing to this project. eVaka project is currently in a very active development phase. Due to this, the core development team may be unable/slow to process or accept contributions at this stage.

When contributing to this repository, please first discuss the change you wish to make via issue, email, or any other method with the owners of this repository before making a change.

## Pull Request Process

1. Ensure all install and build artifacts not intended for the repository have been removed from your commits before opening a Pull Request
2. If your changes warrant or relate to existing out-of-source-code documentation, update the appropriate README.md file(s) with details of the changes, useful examples or new environment variables
3. Ensure all files have appropriate license headers by running the included [helper script](./README.md#Automatically-add-licensing-headers)
4. Open your Pull Request and fill in all the details described in the Pull Request template. Additional testing instructions are always welcome for complex changes.
5. Once you have opened the Pull Request, someone from the team will trigger the [CI workflow for forked Pull Requests](./README.md#Running-CI-for-forked-Pull-Requests) to test your changes.
   **NOTE:** You might not receive automatic notifications from CI failures but reviewers will try to notify you with their best effort.
6. You may merge the Pull Request in once one of the developers have given approving reviews to your Pull Request and all GitHub checks are passing.

## Git Hooks

This repository uses [Lefthook](https://lefthook.dev/) for configuring git hooks. Using git hooks is optional, but useful for ensuring consistency before submitting changes. All the checks included in the supplied hooks are also run as part of the CI workflow.

To enable the hooks, follow these steps:

1. Install the Lefthook [binary](https://lefthook.dev/installation/index.html) using the instructions on the Lefthook website.
2. Install `reuse` [binary](https://reuse.readthedocs.io/en/stable/readme.html#install) for the automatic license check.
3. Run `lefthook install` in the repository root.
