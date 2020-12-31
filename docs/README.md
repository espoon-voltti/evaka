<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka Architecture Documentation

This repository contains the architecture documentation for Espoo early childhood education system (eVaka).

## How to develop

- Architecture diagrams are based on the [C4 model for visualising software architecture](https://c4model.com/) and implemented using [Plant UML](https://plantuml.com/)
- Documentation language is Finnish

### How to generate SVG images from `.puml` files

You can generate PNG, SVG or even AsciiArt files from PlantUML source files
using the command line tool provided by https://plantuml.com

#### Prerequisites
- Java installed on your development box

#### Instructions

1. Download [PlantUML jar](https://search.maven.org/remotecontent?filepath=net/sourceforge/plantuml/plantuml/1.2019.12/plantuml-1.2019.12.jar) from e.g. central Maven repository
2. Generate SVG image from e.g. `source.puml` file by executing following command: `$ java -jar plantuml.jar -tsvg source.puml -o svg/`
3. Check out the generated image from `svg/` directory