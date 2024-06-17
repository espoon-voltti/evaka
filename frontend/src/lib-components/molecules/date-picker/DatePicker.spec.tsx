// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import '@testing-library/jest-dom'
import React, { cloneElement } from 'react'

import LocalDate from 'lib-common/local-date'

import { Translations } from '../../i18n'
import {
  TestContextProvider,
  testTranslations
} from '../../utils/TestContextProvider'

import DatePicker from './DatePicker'

const translations: Translations = {
  ...testTranslations,
  datePicker: {
    placeholder: 'Placeholder',
    description: 'Date picker description',
    validationErrors: {
      validDate: 'Invalid date',
      dateTooEarly: 'Date too early',
      dateTooLate: 'Date too late'
    }
  }
}

describe('DatePicker', () => {
  describe('desktop', () => {
    const jsx = (child: React.JSX.Element) => (
      <TestContextProvider translations={translations}>
        {child}
      </TestContextProvider>
    )
    const get = () =>
      screen.getByRole('textbox', { description: 'Date picker description' })

    it('attributes', () => {
      render(
        jsx(<DatePicker date={null} onChange={() => undefined} locale="fi" />)
      )

      const input = get()
      expect(input).toBeInTheDocument()
      expect(input).toHaveAttribute('placeholder', 'Placeholder')
    })

    it('controlling', () => {
      const onChange = jest.fn()

      const { rerender } = render(
        jsx(<DatePicker date={null} onChange={onChange} locale="fi" />)
      )
      const input = get()
      expect(input).toHaveValue('')
      expect(onChange.mock.calls.length).toEqual(0)

      const date = LocalDate.of(2023, 9, 13)
      rerender(jsx(<DatePicker date={date} onChange={onChange} locale="fi" />))
      expect(input).toHaveValue('13.09.2023')
      expect(onChange.mock.calls.length).toEqual(0)

      // Setting date to null doesn't clear the input
      rerender(jsx(<DatePicker date={null} onChange={onChange} locale="fi" />))
      expect(input).toHaveValue('13.09.2023')
      expect(onChange.mock.calls.length).toEqual(0)
    })

    it('calls onChange for valid and invalid inputs', async () => {
      const date = LocalDate.of(2023, 9, 13)
      const onChange = jest.fn()

      const { rerender } = render(
        jsx(<DatePicker date={null} onChange={onChange} locale="fi" />)
      )

      const input = get()
      await userEvent.type(input, date.format())
      expect(onChange.mock.calls).toEqual([[date]])
      onChange.mockClear()

      rerender(jsx(<DatePicker date={date} onChange={onChange} locale="fi" />))
      await userEvent.type(input, '{backspace}')
      expect(onChange.mock.calls).toEqual([[null]])
    })

    it('does NOT call onChange if value is beyond min/max', async () => {
      const date = LocalDate.of(2023, 9, 13)
      const onChange = jest.fn()

      render(
        jsx(
          <DatePicker
            date={null}
            onChange={onChange}
            locale="fi"
            minDate={date}
            maxDate={date}
          />
        )
      )

      const input = get()
      await userEvent.type(input, date.subDays(1).format())
      await userEvent.clear(input)
      await userEvent.type(input, date.addDays(1).format())
      expect(onChange.mock.calls.length).toEqual(0)
      onChange.mockClear()
    })
  })
  describe('native datepicker (android)', () => {
    const jsx = (child: React.JSX.Element) => (
      <TestContextProvider translations={translations}>
        <label htmlFor="datepicker">Date picker</label>
        {cloneElement(child, { id: 'datepicker', useBrowserPicker: true })}
      </TestContextProvider>
    )
    const get = () => screen.getByLabelText('Date picker')
    const change = (el: HTMLElement, value: string) =>
      fireEvent.change(el, { target: { value } })

    it('attributes', () => {
      render(
        jsx(<DatePicker date={null} onChange={() => undefined} locale="fi" />)
      )

      const input = get()
      expect(input).toBeInTheDocument()
      expect(input).toHaveAttribute('type', 'date')
      expect(input).toHaveAttribute('placeholder', 'Placeholder')
    })

    it('controlling', () => {
      const onChange = jest.fn()

      const { rerender } = render(
        jsx(<DatePicker date={null} onChange={onChange} locale="fi" />)
      )
      const input = get()
      expect(input).toHaveValue('')
      expect(onChange.mock.calls.length).toEqual(0)

      const date = LocalDate.of(2023, 9, 13)
      rerender(jsx(<DatePicker date={date} onChange={onChange} locale="fi" />))
      expect(input).toHaveValue(date.formatIso())
      expect(onChange.mock.calls.length).toEqual(0)

      // Setting date to null doesn't clear the input
      rerender(jsx(<DatePicker date={null} onChange={onChange} locale="fi" />))
      expect(input).toHaveValue(date.formatIso())
      expect(onChange.mock.calls.length).toEqual(0)
    })

    it('calls onChange for valid and invalid inputs', () => {
      const date = LocalDate.of(2023, 9, 13)
      const onChange = jest.fn()

      const { rerender } = render(
        jsx(<DatePicker date={null} onChange={onChange} locale="fi" />)
      )

      const input = get()
      change(input, date.formatIso())
      expect(onChange.mock.calls).toEqual([[date]])
      onChange.mockClear()

      rerender(jsx(<DatePicker date={date} onChange={onChange} locale="fi" />))
      change(input, '')
      expect(onChange.mock.calls).toEqual([[null]])
    })

    it('does NOT call onChange if value is beyond min/max', () => {
      const date = LocalDate.of(2023, 9, 13)
      const onChange = jest.fn()

      render(
        jsx(
          <DatePicker
            date={null}
            onChange={onChange}
            locale="fi"
            minDate={date}
            maxDate={date}
          />
        )
      )

      const input = get()
      change(input, date.subDays(1).formatIso())
      change(input, '')
      change(input, date.addDays(1).formatIso())
      expect(onChange.mock.calls.length).toEqual(0)
      onChange.mockClear()
    })
  })
})
