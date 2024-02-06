// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from './json'
import LocalHm from './local-hm'

export default class LocalHmRange {
  constructor(
    public readonly start: LocalHm,
    public readonly end: LocalHm
  ) {
    if (end.isBefore(start)) {
      throw new Error(
        `Attempting to initialize invalid minute resolution time range with start: ${start.format()}, end: ${end.format()}`
      )
    }
  }

  static parseJson(json: JsonOf<LocalHmRange>): LocalHmRange {
    return new LocalHmRange(LocalHm.parse(json.start), LocalHm.parse(json.end))
  }
}
