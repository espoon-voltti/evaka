// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export default {
  getCountries: (locale) =>
    import(`@/localization/countries/${locale}.json`).then((countries) =>
      convertToNameValueList(countries.default)
    ),

  getLanguages: (locale) =>
    import(`@/localization/languages/${locale}.json`).then((languages) =>
      convertToNameValueList(languages.default)
    )
}

const convertToNameValueList = (source) =>
  Object.entries(source).map((entry) => ({ name: entry[0], value: entry[1] }))
