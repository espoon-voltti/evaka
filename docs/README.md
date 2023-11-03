<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka Architecture Documentation

This directory contains the architecture documentation for Espoo early childhood education system (eVaka).

- Architecture diagrams are based on the [C4 model for visualising software architecture](https://c4model.com/) and implemented using [Plant UML](https://plantuml.com/)
- Documentation language is Finnish

## Generate SVG images from `.puml` files

You can generate PNG, SVG or even AsciiArt files from [PlantUML](https://plantuml.com) source files:

1. [using Docker](#using-docker) (recommended, only requires Docker)
1. [using the command line tool](#using-the-command-line-tool) (more customizable, requires Java etc.)

### Using Docker

1. Install [Docker](https://docs.docker.com/engine/install/)
1. `./convert.sh`
1. Check out the generated images in `./diagrams/svg/` directory

### Using the command line tool

1. Install Java (e.g. [OpenJDK](https://openjdk.java.net/install/))
1. Install [Graphviz](https://graphviz.org/)
1. Download [PlantUML jar](https://search.maven.org/remotecontent?filepath=net/sourceforge/plantuml/plantuml/1.2023.12/plantuml-1.2023.12.jar) (version `1.2023.12`) from e.g. central Maven repository
1. Generate SVG image from e.g. `source.puml` file by executing following command: `$ java -jar plantuml.jar -tsvg source.puml -o diagrams/svg/`
1. Check out the generated image from `diagrams/svg/` directory

See also: [official documentation for the command line tool](https://plantuml.com/command-line)

# eVaka database schema

[eVaka database schema diagram](./evaka_db_schema.png)

## Vasu schema

Early childhood education plan (vasu, lapsen varhaiskasvatussuunnitelma in Finnish) documents are stored mostly in json format. This dynamic structure allows the admin user
to modify vasu templates so that the sections and questions can vary from year to year and include content specific to the
given city.

The most up-to-date schema can be found from the related kotlin [data classes](../service/src/main/kotlin/fi/espoo/evaka/vasu/Vasu.kt).

Example json of a new draft document with default questions was last exported 20.9.2021 (subject to change):
[Examples of vasu json content](./vasu-json-examples)
