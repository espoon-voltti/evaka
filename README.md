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

- [`apigw`](apigw/) – API gateway, responsible for handling authentication and routing requests to backend services
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

## Contact information
