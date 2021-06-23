import { stringToNumber } from './number'

describe('stringToNumber', () => {
  it('works', () => {
    expect(stringToNumber('123')).toEqual(123)
    expect(stringToNumber('123.45')).toEqual(123.45)
    expect(stringToNumber('0,45')).toEqual(0.45)

    expect(stringToNumber('')).toBeUndefined()
    expect(stringToNumber('lol')).toBeUndefined()
    expect(stringToNumber('123asdf')).toBeUndefined()
    expect(stringToNumber('111.ars')).toBeUndefined()
    expect(stringToNumber('1e2')).toBeUndefined()
    expect(stringToNumber('.123')).toBeUndefined()
    expect(stringToNumber('123.123.123')).toBeUndefined()
  })
})
