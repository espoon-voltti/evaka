// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * Produces a mutable deep copy of the original object
 */
// oxlint-disable-next-line typescript/no-explicit-any
export const deepCopyObj = (obj: any): any => JSON.parse(JSON.stringify(obj))
