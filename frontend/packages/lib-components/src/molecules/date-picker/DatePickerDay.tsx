import React from 'react'
import DayPicker from 'react-day-picker'
import 'react-day-picker/lib/style.css'

import LocalDate from '@evaka/lib-common/src/local-date'

const MONTHS = [
  'tammikuu',
  'helmikuu',
  'maaliskuu',
  'huhtikuu',
  'toukokuu',
  'kesäkuu',
  'heinäkuu',
  'elokuu',
  'syyskuu',
  'lokakuu',
  'marraskuu',
  'joulukuu'
]
const WEEKDAYS_LONG = [
  'sunnuntai',
  'maanantai',
  'tiistai',
  'keskiviikko',
  'torstai',
  'perjantai',
  'lauantai'
]
const WEEKDAYS_SHORT = ['su', 'ma', 'ti', 'ke', 'to', 'pe', 'la']

interface Props {
  handleDayClick: (day: Date) => void
  inputValue: string
}

function DatePickerDay({ handleDayClick, inputValue }: Props) {
  function convertToDate(date: string) {
    try {
      return LocalDate.parseFi(date).toSystemTzDate()
    } catch (e) {
      return undefined
    }
  }

  return (
    <DayPicker
      onDayClick={handleDayClick}
      locale="fi"
      months={MONTHS.map(
        (month) => month.charAt(0).toUpperCase() + month.slice(1)
      )}
      weekdaysLong={WEEKDAYS_LONG.map(
        (month) => month.charAt(0).toUpperCase() + month.slice(1)
      )}
      weekdaysShort={WEEKDAYS_SHORT.map(
        (month) => month.charAt(0).toUpperCase() + month.slice(1)
      )}
      firstDayOfWeek={1}
      selectedDays={convertToDate(inputValue)}
    />
  )
}

export default DatePickerDay
