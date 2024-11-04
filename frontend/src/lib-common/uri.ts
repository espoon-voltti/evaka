// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

type PathParam = Uri | string | number | boolean

const encode = (value: PathParam) => encodeURIComponent(value.toString())

function* uriParts(
  templateIter: Iterator<string>,
  paramIter: Iterator<PathParam>
): Generator<string> {
  yield templateIter.next().value

  let param = paramIter.next()
  let template = templateIter.next()
  while (!param.done && !template.done) {
    yield param.value instanceof Uri
      ? param.value.toString()
      : encode(param.value)
    yield template.value
    param = paramIter.next()
    template = templateIter.next()
  }
}

class Uri {
  constructor(readonly value: string) {}

  appendQuery(params: URLSearchParams): Uri {
    return params.size > 0 ? new Uri(`${this.value}?${params}`) : this
  }

  toString(): string {
    return this.value
  }
}

// export only the type so calling new Uri(...) is impossible, but the type can be used in signatures
export type { Uri }
export const uri = (
  template: TemplateStringsArray,
  ...params: PathParam[]
): Uri =>
  new Uri(
    Array.from(
      uriParts(template[Symbol.iterator](), params[Symbol.iterator]())
    ).join('')
  )
