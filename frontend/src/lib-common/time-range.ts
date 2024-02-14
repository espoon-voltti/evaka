// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from './json'
import LocalTime from './local-time'

export default class TimeRange {
  constructor(
    public start: LocalTime,
    public end: LocalTime
  ) {
    if (start.isEqualOrAfter(end) && end !== LocalTime.MIDNIGHT) {
      throw new Error('TimeRange start must be before end')
    }
  }

  includes(time: LocalTime): boolean {
    return time.isEqualOrAfter(this.start) && time.isBefore(this.end)
  }

  /** HH:mm:ss */
  static parseJson(json: JsonOf<TimeRange>): TimeRange {
    return new TimeRange(
      LocalTime.parseIso(json.start),
      LocalTime.parseIso(json.end)
    )
  }

  /** HH:mm */
  static parse(json: JsonOf<TimeRange>): TimeRange {
    return new TimeRange(LocalTime.parse(json.start), LocalTime.parse(json.end))
  }

  toJSON(): JsonOf<TimeRange> {
    return {
      start: this.start.toJSON(),
      end: this.end.toJSON()
    }
  }
}
