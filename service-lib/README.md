<!--
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# service-lib

A collection of library-like functionalities for eVaka Kotlin services, such
as a common logger and JWT authentication.

## Development

Building and testing is done as a part of the using Kotlin service builds,
i.e. evaka-service and evaka-message-service, using this project as a
sub-project dependency.

## Known issues

- [ ] Multiple Logback `ConsoleAppender`s could theoretically corrupt
    each other's JSON logs if more than one of them output at the same time
    (w/ multiline content)
