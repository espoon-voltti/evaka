import { formatDate } from './date'

describe('formatDate', () => {
  it('should format valid date correctly', () => {
    expect(formatDate(new Date('2019-01-01'))).toBe('01.01.2019')
  })

  it('should format undefined to empty string', () => {
    expect(formatDate(undefined)).toBe('')
  })
})
