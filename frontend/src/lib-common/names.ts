// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const formatPreferredName = (person: {
  firstName: string
  preferredName: string | null
}) => person.preferredName || person.firstName.split(' ')[0]
