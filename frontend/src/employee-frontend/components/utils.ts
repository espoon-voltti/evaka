// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function textAreaRows(
  text: string,
  minRows = 1,
  maxRows = 15,
  lineLength = 62
): number {
  const lines = text.length / lineLength + (text.split(/\n/).length - 1)
  return Math.ceil(Math.max(minRows, Math.min(maxRows, lines)))
}
