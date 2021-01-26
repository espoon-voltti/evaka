// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'

export interface LabeledValue {
  value: string
  label: string
}

export const LANGUAGES = {
  FI: 'fi',
  SV: 'sv',
  EN: 'en'
}

export const SERVICE_NAMES = {
  centre: 'Päiväkoti',
  family: 'Perhepäivähoito',
  group_family: 'Ryhmäperhepäiväkoti',
  club: 'Kerho',
  preschool: 'Esiopetus'
}

export const DAYCARE_TYPES = _.map(SERVICE_NAMES, (value, key) => {
  return { value: key, label: value }
})

export const SERVICE_CLASSES = {
  CENTRE: 'centre',
  FAMILY: 'family',
  GROUP_FAMILY: 'group',
  CLUB: 'club',
  PRESCHOOL: 'preschool'
}

export const UNIT_TYPE = {
  DAYCARE: 'CENTRE',
  CLUB: 'CLUB',
  PRESCHOOL: 'PRESCHOOL',
  PREPARATORY_EDUCATION: 'PREPARATORY_EDUCATION'
}

export const WEEKLY_HOURS = {
  OVER_35: { value: 'over_35', label: 'Väh. 35 tuntia' },
  BETWEEN_25_35: { value: 'between_25_and_35', label: '25 – 35 tuntia' },
  MAX_25: { value: 'under_25', label: 'Maks. 25 tuntia' }
}

export const CLUB_HOURS = {
  TWO_HOURS: { value: '2', label: '2 tuntia' },
  THREE_HOURS: { value: '3', label: '3 tuntia' }
}

export const CLUB_DAYS = {
  TWO_DAYS: { value: '2', label: '2 päivää' },
  THREE_DAYS: { value: '3', label: '3 päivää' }
}

export const APPLICATION_TYPE = {
  DAYCARE: { value: 'DAYCARE', label: 'Varhaiskasvatus' },
  PRESCHOOL: { value: 'PRESCHOOL', label: 'Esiopetus' },
  CLUB: { value: 'CLUB', label: 'Kerho' }
}

export const WORK_STATUS = {
  EMPLOYED: { value: 'employed', label: 'Töissä' },
  UNEMPLOYED: { value: 'unemployed', label: 'Työtön' },
  STUDENT: { value: 'student', label: 'Opiskelija' }
}

export const APPLICATION_STATUS = {
  CREATED: { value: 'CREATED', label: 'Luonnos' },
  SENT: { value: 'SENT', label: 'Lähetetty' },
  VERIFIED: { value: 'VERIFIED', label: 'Käsiteltävänä' },
  WAITING_PLACEMENT: { value: 'WAITING_PLACEMENT', label: 'Käsiteltävänä' },
  WAITING_UNIT_CONFIRMATION: { value: 'WAITING_UNIT_CONFIRMATION', label: 'Käsiteltävänä' },
  WAITING_DECISION: { value: 'WAITING_DECISION', label: 'Käsiteltävänä' },
  PLACEMENT_PROPOSED: { value: 'PLACEMENT_PROPOSED', label: 'Käsiteltävänä' },
  PLACEMENT_QUEUED: { value: 'PLACEMENT_QUEUED', label: 'Käsiteltävänä' },
  CLEARED_FOR_CONFIRMATION: {
    value: 'CLEARED_FOR_CONFIRMATION',
    label: 'Vahvistettavana huoltajalla'
  },
  WAITING_CONFIRMATION: {
    value: 'WAITING_CONFIRMATION',
    label: 'Vahvistettavana huoltajalla'
  },
  ACTIVE: { value: 'ACTIVE', label: 'Paikka vastaanotettu' },
  ACCEPTED: { value: 'ACCEPTED', label: 'Paikka vastaanotettu' },
  REJECTED: { value: 'REJECTED', label: 'Paikka hylätty' },
  CANCELLED: { value: 'CANCELLED', label: 'Poistettu käsittelystä' },
  TERMINATED: { value: 'TERMINATED', label: 'Irtisanottu' }
}

export const MIN_PREFERRED_UNITS = 1
export const MAX_PREFERRED_UNITS = 3

export const DATE_FORMAT_HOURS = 'HH:mm:ss'
export const DATE_FORMAT = 'dd.MM.yyyy'
export const DATE_FORMAT_INSTANT = 'dd.MM.yyyy HH:mm:ss'
export const DATE_FORMAT_DEFAULT = 'yyyy-MM-dd'
export const DATE_FORMAT_MINUTES = 'HH:mm'
export const DATE_FORMAT_MASK = [
  /\d/,
  /\d/,
  '.',
  /\d/,
  /\d/,
  '.',
  /\d/,
  /\d/,
  /\d/,
  /\d/
]
export const DATE_FORMAT_REGEX = /^\d{2}[.]\d{2}[.]\d{4}$/

export function applicationTypeToDaycareTypes(type) {
  switch (type) {
    case APPLICATION_TYPE.DAYCARE:
      return ['CENTRE', 'FAMILY', 'GROUP_FAMILY']
    case APPLICATION_TYPE.PRESCHOOL:
      return ['PRESCHOOL']
    case APPLICATION_TYPE.CLUB:
      return ['CLUB']
    default:
      return []
  }
}

/**
 * Helper to create select lists of constant objects e.g.
 *
 * <select-button-group
 *  :value="CONSTANT"
 *  :values="_.map(CONSTANT, toSelectValue)"
 * >
 *
 * @param constant
 * @returns {{value: *, label}}
 */
export function toSelectValue(value) {
  return { value, label: value.label }
}

export const NORMAL_CARE_START = '06:30'
export const NORMAL_CARE_END = '18:00'

export const VCalendarConfig = {
  firstDayOfWeek: 2,
  locale: 'fi',
  formats: {
    title: 'MMMM YYYY',
    weekdays: 'W',
    navMonths: 'MMM',
    input: ['DD.MM.YYYY', 'YYYY-MM-DD'],
    dayPopover: 'L',
    data: ['DD.MM.YYYY', 'YYYY-MM-DD']
  }
}

export const DATE_PICKER_STYLE = {
  wrapper: {
    border: '0' // Override the default border
  },
  header: {
    color: '#fafafa',
    backgroundColor: '#3273dc',
    borderColor: '#404c59',
    borderWidth: '1px 1px 0 1px'
  },
  headerVerticalDivider: {
    borderLeft: '1px solid #404c59'
  },
  weekdays: {
    color: '#bcd5ff',
    backgroundColor: '#3273dc',
    borderColor: '#384763',
    borderWidth: '0 1px',
    paddingTop: '5px',
    paddingBottom: '10px'
  },
  weekdaysVerticalDivider: {
    borderLeft: '1px solid #404c59'
  },
  weeks: {
    border: '1px solid #dadada'
  }
}

// Will be set to season 2021 start date at some point around new year
export const PRESCHOOL_START_DATE_FI: string = ''
export const PRESCHOOL_START_DATE_SV: string = ''

export const PROVIDER_TYPE = {
  MUNICIPAL: 'MUNICIPAL',
  PURCHASED: 'PURCHASED',
  PRIVATE: 'PRIVATE',
  MUNICIPAL_SCHOOL: 'MUNICIPAL_SCHOOL',
  PRIVATE_SERVICE_VOUCHER: 'PRIVATE_SERVICE_VOUCHER'
}

export const ATTACHMENT_TYPE = {
  URGENCY: 'URGENCY',
  EXTENDED_CARE: 'EXTENDED_CARE'
}
