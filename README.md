<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka – ERP for early childhood education

[![REUSE status](https://api.reuse.software/badge/github.com/espoon-voltti/evaka)](https://api.reuse.software/info/github.com/espoon-voltti/evaka)

<!-- This project is registered with the REUSE API: https://api.reuse.software/ -->

This is eVaka – an ERP system developed for early childhood education in
Finland. It is a web application developed using modern technologies and
designed to be deployed in cloud environment.

**Caution:** eVaka is currently under very active development. At this
point, it is not yet adviced to start developing or running your custom
solution based on this project. Most likely, the project will evolve
considerably before reaching more stable phase. Please refer to
project's roadmap for further information regarding development plans.
However, feel free to explore the project and try running it locally.

## How to get started

For development purposes, the application can be run locally on your
development machine. See further instructions in the
[README](compose/README.md) file of the `compose/` directory.

The applications consists of several sub-projects:

- [`apigw`](apigw/) – API gateway, responsible for handling authentication and
  routing requests to backend services
- [`frontend`](frontend/) – User interfaces for the citizens and
  employees, and [Nginx](https://www.nginx.com) image for proxying
  frontend requests to services
- [`service`](service/) – All the business logic, e.g. handling early
  childhood education application process and daily work in daycare
- [`compose`](compose/) – Tooling for running application in local
  development environment, using e.g. [Docker](https://www.docker.com)
- [`service/service-lib`](service/service-lib/) – Old common library for service projects,
  but today only used by `service`.

and a few other important directories:

- [`bin`](bin/) – Helper shell scripts
- [`docs`](docs/) – General eVaka documentation
- [`service/evaka-bom`](service/evaka-bom/) – Shared BOM for eVaka Gradle projects, for
  easier dependency updates and control

## Contributing

Please refer to [CONTRIBUTING.md](CONTRIBUTING.md) for further
instructions regarding code contributions.

### Running CI for forked Pull Requests

**After** the code in a forked PR has been reviewed to not pose a security risk
for CI:

In CircleCI, "Build forked pull requests" must be enabled for repository.

**IMPORTANT:** "Pass secrets to builds from forked pull requests" must still be
**disabled**.

```sh
# Add remote (can be done only once if expecting more PRs from same remote)
git remote add <name for remote> <address of remote>
# E.g.: git remote add Tampere git@github.com:Tampere/evaka.git

# Fetch remote references
git fetch --all

# Push refs as-is to origin
# NOTE: Upstream branch name doesn't need to match downstream, just the refs
# need to be identical
git push --force origin "refs/remotes/<name for remote>/<source branch>:refs/heads/<upstream branch>"
# E.g. git push --force origin "refs/remotes/Tampere/cool-branch-sauce:refs/heads/cool-branch-sauce"

# Remove remote (can be skipped if expecting more PRs from same remote)
git remote remove <name for remote>
# E.g. git remote remove Tampere
```

After the push the build in the original PR continues. Once it passes, merge the branch.

If any changes are made to the PR, they must similarly pushed to origin. You
can use the same method as above.

## Setup Github Actions

Github Action workflows can be enabled to repository from Github project -> Actions and enable each workflow.

By default the workflow does not use cache or construct any artifacts.

To enable features add variables and secrets from Github Project -> Settings -> Secrets and variables -> Actions

| Variable             | Value         | Required secrets                        |                                        |
|----------------------|---------------|-----------------------------------------|----------------------------------------|
| `AWS`                | `true`        | `AWS_ROLE`, `AWS_REGION`                | Where `AWS_ROLE` is AWS OIDC ARN.      |
| `S3`                 | `true`        |                                         |                                        |
| `DOCKERHUB`          | `true`        | `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` | Only read access needed                |
| `DEPLOY`             | `true`        | `EVAKA_PAT`                             | Requires `evaka-deploy` repository     |
| `SLACK`              | `true`        | `SLACK_WEBHOOK_URL`                     |                                        |
| `OWASP`              | `true`        |                                         |                                        |
| `FONTAWESOME`        | `true`        | `FONTAWESOME_TOKEN`                     |                                        |
| *enabled by secret*  |               | `SENTRY_AUTH_TOKEN`                     |                                        |

Most of the features need `AWS` feature to function.

## Security issues

In case you notice any information security related issue in our
services, or you have an idea how to improve information security,
please contact us by email at [tietoturva@espoo.fi](tietoturva@espoo.fi)

## License

eVaka is published under **LGPL-2.1-or-later** license. Please refer to
[LICENSE](LICENSE) for further details.

### Bulk-licensing

Bulk-licensing is applied to certain directories that will never contain
anything but binary-like files (e.g. certificates) with
[a DEP5 file](./.reuse/dep5) (see
[docs](https://reuse.software/faq/#bulk-license)).

### Check licensing compliance

This repository targets [REUSE](https://reuse.software/) compliance by utilizing
the [reuse CLI tool](https://git.fsfe.org/reuse/tool) and the
[REUSE API](https://api.reuse.software/).

The REUSE API constantly checks this repository's compliance and the status
can be seen from the badge at the top of this README.

To manually check that the repository is compliant (e.g. before submitting a pull
request), run:

```sh
./bin/add-license-headers.sh --lint-only

# See also:
./bin/add-license-headers.sh --help
```

**NOTE:** The tool has no concept for "no license" -> all files must indicate
their license explicitly (or using bulk licensing). And if files cannot be
licensed, they shouldn't be included in this repository at all.

### Automatically add licensing headers

To **attempt** automatically adding licensing headers to all source files, run:

```sh
./bin/add-license-headers.sh
```

**NOTE:** The script uses the [reuse CLI tool](https://git.fsfe.org/reuse/tool),
which has limited capability in recognizing file types but will give some
helpful output in those cases, like:

```sh
$ ./bin/add-license-headers.sh
usage: reuse addheader [-h] [--copyright COPYRIGHT] [--license LICENSE]
                       [--year YEAR]
                       [--style {applescript,aspx,bibtex,c,css,haskell,html,jinja,jsx,lisp,m4,ml,python,tex}]
                       [--template TEMPLATE] [--exclude-year] [--single-line]
                       [--multi-line] [--explicit-license]
                       [--skip-unrecognised]
                       path [path ...]
reuse addheader: error: 'frontend/packages/employee-frontend/src/components/voucher-value-decision/VoucherValueDecisionActionBar.tsx' does not have a recognised file extension, please use --style, --explicit-license or --skip-unrecognised
```

### Linting shell scripts

CI does this automatically but you can manually lint all the shell scripts in
this repo with: `./bin/run-shellcheck.sh`. See [shellcheck](https://github.com/koalaman/shellcheck)
for more details on the linter.

## Contact information

eVaka is developed in the [City of Espoo](https://www.espoo.fi), Finland.
You can contact the development team at [voltti@espoo.fi](voltti@espoo.fi).
