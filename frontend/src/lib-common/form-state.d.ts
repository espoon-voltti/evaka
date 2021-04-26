// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type UpdateStateFn<T> = <K extends keyof T>(values: Pick<T, K>) => void
