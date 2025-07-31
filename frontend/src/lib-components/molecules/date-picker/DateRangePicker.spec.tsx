// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import '@testing-library/jest-dom'
import React from 'react'

import LocalDate from 'lib-common/local-date'

import type { Translations } from '../../i18n'
import {
  TestContextProvider,
  testTranslations
} from '../../utils/TestContextProvider'

import DateRangePicker from './DateRangePicker'

const translations: Translations = {
  ...testTranslations,
  datePicker: {
    placeholder: 'Placeholder',
    description: 'Date picker description',
    validationErrors: {
      validDate: 'Invalid date',
      dateTooEarly: 'Date too early',
      dateTooLate: 'Date too late'
    },
    open: 'Open date picker',
    close: 'Close date picker'
  }
}

describe('DateRangePicker', () => {
  it('renders 2 date pickers', () => {
    render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={null}
          end={null}
          onChange={() => undefined}
          locale="fi"
        />
      </TestContextProvider>
    )

    const [start, end] = screen.getAllByRole('textbox', {
      name: 'Date picker description'
    })
    expect(start).toHaveAttribute('placeholder', 'Placeholder')
    expect(end).toHaveAttribute('placeholder', 'Placeholder')
  })

  it('controlling', () => {
    const date1 = LocalDate.of(2023, 9, 1)
    const date2 = LocalDate.of(2023, 9, 2)
    const date3 = LocalDate.of(2023, 9, 3)
    const onChange = jest.fn()

    const { rerender } = render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={null}
          end={null}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )

    const [start, end] = screen.getAllByRole('textbox')
    expect(start).toHaveValue('')
    expect(end).toHaveValue('')

    rerender(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={date1}
          end={null}
          onChange={() => undefined}
          locale="fi"
        />
      </TestContextProvider>
    )
    expect(start).toHaveValue(date1.format())
    expect(end).toHaveValue('')
    expect(onChange.mock.calls.length).toEqual(0)

    rerender(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={date1}
          end={date2}
          onChange={() => undefined}
          locale="fi"
        />
      </TestContextProvider>
    )
    expect(start).toHaveValue(date1.format())
    expect(end).toHaveValue(date2.format())
    expect(onChange.mock.calls.length).toEqual(0)

    rerender(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={date3}
          end={date2}
          onChange={() => undefined}
          locale="fi"
        />
      </TestContextProvider>
    )
    expect(start).toHaveValue(date3.format())
    expect(end).toHaveValue(date2.format())
    expect(onChange.mock.calls.length).toEqual(0)

    // Setting start or end to null doesn't clear the respective input
    rerender(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={null}
          end={null}
          onChange={() => undefined}
          locale="fi"
        />
      </TestContextProvider>
    )
    expect(start).toHaveValue(date3.format())
    expect(end).toHaveValue(date2.format())
    expect(onChange.mock.calls.length).toEqual(0)
  })

  it('calls onChange for valid and invalid inputs', async () => {
    const date1 = LocalDate.of(2023, 9, 1)
    const date2 = LocalDate.of(2023, 9, 2)
    const date3 = LocalDate.of(2023, 9, 3)

    const onChange = jest.fn()

    const { rerender } = render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={null}
          end={null}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )

    const [start, end] = screen.getAllByRole('textbox')
    await userEvent.type(start, date1.format())
    expect(onChange.mock.calls).toEqual([[date1, null]])
    onChange.mockClear()

    rerender(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={date1}
          end={null}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )
    await userEvent.type(end, date2.format())
    expect(onChange.mock.calls).toEqual([[date1, date2]])
    onChange.mockClear()

    rerender(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={date1}
          end={date2}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )
    await userEvent.click(end)
    await userEvent.keyboard('{backspace}')
    expect(onChange.mock.calls).toEqual([[date1, null]])
    onChange.mockClear()

    rerender(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={date1}
          end={null}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )
    await userEvent.clear(end)
    await userEvent.type(end, date3.format())
    expect(onChange.mock.calls).toEqual([[date1, date3]])
    onChange.mockClear()
  })

  it('does NOT call onChange if dates are beyond min/max', async () => {
    const date = LocalDate.of(2023, 9, 1)
    const onChange = jest.fn()

    render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={null}
          end={null}
          onChange={onChange}
          minDate={date}
          maxDate={date}
          locale="fi"
        />
      </TestContextProvider>
    )

    const [start, end] = screen.getAllByRole('textbox')
    await userEvent.type(start, date.subDays(1).format())
    await userEvent.clear(start)
    await userEvent.type(end, date.addDays(1).format())
    expect(onChange.mock.calls).toEqual([])
    onChange.mockClear()
  })

  it('does NOT call onChange if dates are invalid', async () => {
    const date = LocalDate.of(2023, 9, 1)
    const onChange = jest.fn()

    render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={null}
          end={null}
          onChange={onChange}
          isInvalidDate={(localDate) =>
            localDate.isEqual(date) ? null : 'Invalid'
          }
          locale="fi"
        />
      </TestContextProvider>
    )

    const [start, end] = screen.getAllByRole('textbox')
    await userEvent.type(start, date.subDays(1).format())
    await userEvent.clear(start)
    await userEvent.type(end, date.addDays(1).format())
    expect(onChange.mock.calls).toEqual([])
    onChange.mockClear()
  })

  it('if start passes end, sets end to start', async () => {
    const dateBefore = LocalDate.of(2023, 9, 1)
    const dateAfter = dateBefore.addDays(5)
    const onChange = jest.fn()

    render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={null}
          end={dateBefore}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )

    const [start] = screen.getAllByRole('textbox')
    await userEvent.type(start, dateAfter.format())
    expect(onChange.mock.calls).toEqual([[dateAfter, dateAfter]])
    onChange.mockClear()
  })

  it('if end precedes start, sets start to end', async () => {
    const dateBefore = LocalDate.of(2023, 9, 1)
    const dateAfter = dateBefore.addDays(5)
    const onChange = jest.fn()

    render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={dateAfter}
          end={null}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )

    const [_, end] = screen.getAllByRole('textbox')
    await userEvent.type(end, dateBefore.format())
    expect(onChange.mock.calls).toEqual([[dateBefore, dateBefore]])
    onChange.mockClear()
  })

  it('start and end can be set to invalid order using props', () => {
    const dateBefore = LocalDate.of(2023, 9, 1)
    const dateAfter = dateBefore.addDays(5)
    const onChange = jest.fn()

    render(
      <TestContextProvider translations={translations}>
        <DateRangePicker
          start={dateAfter}
          end={dateBefore}
          onChange={onChange}
          locale="fi"
        />
      </TestContextProvider>
    )

    const [start, end] = screen.getAllByRole('textbox')
    expect(start).toHaveValue(dateAfter.format())
    expect(end).toHaveValue(dateBefore.format())
  })
})
