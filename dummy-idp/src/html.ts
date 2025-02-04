// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export class Html {
  constructor(private text: string) {}
  toString() {
    return this.text
  }
}

// Poor man's non-production-ready HTML escaping with template strings
export const html = (
  template: TemplateStringsArray,
  ...params: (string | number | Html | Html[])[]
): Html =>
  new Html(
    Array.from(
      htmlParts(template[Symbol.iterator](), params[Symbol.iterator]())
    ).join('')
  )

function* htmlParts(
  templateIter: Iterator<string>,
  paramIter: Iterator<string | number | Html | Html[]>
): Generator<string> {
  yield templateIter.next().value

  let param = paramIter.next()
  let template = templateIter.next()
  while (!param.done && !template.done) {
    yield param.value instanceof Html
      ? param.value.toString()
      : Array.isArray(param.value)
        ? param.value.join('\n')
        : param.value.toString().replace(/[&"'<>]/g, (char) => {
            switch (char) {
              case '&':
                return '&amp;'
              case '"':
                return '&quot;'
              case "'":
                return '&apos;'
              case '<':
                return '&lt;'
              case '>':
                return '&gt;'
              default:
                return char
            }
          })
    yield template.value
    param = paramIter.next()
    template = templateIter.next()
  }
}
