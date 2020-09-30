// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* English locals for flatpickr */
const Flatpickr = Flatpickr || { l10ns: {} }
Flatpickr.l10ns.en = {}

Flatpickr.l10ns.en.firstDayOfWeek = 1
Flatpickr.l10ns.en.weekAbbreviation = 'Wk'

Flatpickr.l10ns.en.weekdays = {
  shorthand: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
  longhand: [
    'Sunday',
    'Monday',
    'Tuesday',
    'Wednesday',
    'Thursday',
    'Friday',
    'Saturday'
  ]
}

Flatpickr.l10ns.en.months = {
  shorthand: [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec'
  ],
  longhand: [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December'
  ]
}

Flatpickr.l10ns.en.ordinal = function() {
  return '.'
}

if (typeof module !== 'undefined') module.exports = Flatpickr.l10ns
