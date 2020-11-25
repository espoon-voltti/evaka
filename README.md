<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka – ERP for early childhood education

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
  employees
- [`service`](service/) – All the business logic, e.g. handling early
  childhood education application process and daily work in daycare
- [`message-service`](message-service/) – Service for sending messages
  via [Suomi.fi Messages](https://www.suomi.fi/messages) service
- [`compose`](compose/) – Tooling for running application in local
  development environment, using e.g. [Docker](https://www.docker.com)
- [`proxy`](proxy/) – [Nginx](https://www.nginx.com) image for proxying
  fronetend requests to services
- [`service-lib`](service-lib/) – A common library shared with `service` and
  `message-service` projects

## Contributing

Please refer to [CONTRIBUTING.md](CONTRIBUTING.md) for further
instructions regarding code contributions.

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
the [reuse CLI tool](https://git.fsfe.org/reuse/tool).

To check that the repository is compliant (e.g. before submitting a pull
request), run:

```sh
./bin/add-license-headers.sh --lint-only

# See also:
./bin/add-license-headers.sh --help
```

**NOTE:** The tool has no concept for "no license", so currently it will
always fail for the following files:

- [Digital and population data services agency (DVV)](https://dvv.fi/henkiloasiakkaat)
  owned WSDL and related files (`message-service/**/wsdl/*`, `service/**/wsdl/*`)
- All logos of the city of Espoo (`service/src/main/resources/static/espoo-logo*.png`)

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

## Contact information

eVaka is developed in the [City of Espoo](https://www.espoo.fi), Finland.
You can contact the development team at [voltti@espoo.fi](voltti@espoo.fi).
