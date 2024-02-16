// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// ABSTRACT TYPES

// Helper for Readonly types with depth until Typescript natively supports them
// See: https://github.com/microsoft/TypeScript/issues/13923
// eslint-disable-next-line @typescript-eslint/ban-types
export type primitive = string | number | boolean | undefined | null | Function
export type DeepReadonly<T> = T extends primitive ? T : DeepReadonlyObject<T>
export type DeepReadonlyObject<T> = {
  readonly [P in keyof T]: DeepReadonly<T[P]>
}
export type OmitInUnion<T, K extends keyof T> = T extends T ? Omit<T, K> : never

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type Arg0<T extends (...args: any) => any> = Parameters<T>[0]

// CONCRETE TYPES

export interface BaseAppConfig {
  sentry: {
    dsn: string
    enabled: boolean
  }
}

export type UUID = string
