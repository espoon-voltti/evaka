// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'

export type VtjPerson = z.infer<typeof vtjPersonSchema>
export const vtjPersonSchema = z.strictObject({
  ssn: z.string(),
  givenName: z.string(),
  commonName: z.string(),
  surname: z.string(),
  comment: z.string().optional()
})

// Real Suomi.fi authentication returns these fields.
// These used to be documented somewhere, but the new kehittajille.suomi.fi documentation site has no info about them
export const sfiSamlAttrs = {
  'urn:oid:1.2.246.21': 'nationalIdentificationNumber',
  'urn:oid:2.5.4.3': 'cn',
  'urn:oid:2.16.840.1.113730.3.1.241': 'displayName',
  'urn:oid:2.5.4.42': 'givenName',
  'urn:oid:2.5.4.4': 'sn'
}
export type SfiSamlAttrUrn = keyof typeof sfiSamlAttrs
export const sfiSamlAttrUrns: SfiSamlAttrUrn[] = Object.keys(
  sfiSamlAttrs
) as SfiSamlAttrUrn[]

export function toSfiSamlAttrs(
  person: VtjPerson
): Record<SfiSamlAttrUrn, string> {
  return {
    'urn:oid:1.2.246.21': person.ssn,
    'urn:oid:2.5.4.3': person.commonName,
    'urn:oid:2.16.840.1.113730.3.1.241': `${person.givenName} ${person.surname}`,
    'urn:oid:2.5.4.42': person.givenName,
    'urn:oid:2.5.4.4': person.surname
  }
}

declare module 'express-session' {
  interface SessionData {
    user?: {
      nameId: string
      person: VtjPerson
    }
  }
}

// Default set of VTJ persons, used in local eVaka development.
// These should match evaka-service development data to avoid confusion.
export const defaultPersons: VtjPerson[] = [
  {
    ssn: '231090-912J',
    commonName: 'Liisa Maria Anneli',
    givenName: 'Liisa',
    surname: 'Finström',
    comment: '2 huollettavaa'
  },
  {
    ssn: '010170-999R',
    commonName: 'Tero Petteri Mikael',
    givenName: 'Tero',
    surname: 'Testaaja',
    comment: '2 huollettavaa'
  },
  {
    ssn: '091171-9975',
    commonName: 'Helena Marjatta Sofia',
    givenName: 'Helena',
    surname: 'Huittinen',
    comment: '2 huollettavaa'
  },
  {
    ssn: '050391-999B',
    commonName: 'Parkkonen',
    givenName: 'Parkkonen',
    surname: 'Äåöàáâãæçð Èèéêë-Ììíîïñòóôõ Øøßþùúûüýÿ',
    comment: '2 huollettavaa'
  },
  {
    ssn: '090693-990K',
    commonName: 'Tapio Antero Kalevi',
    givenName: 'Tapio',
    surname: 'Joensuu',
    comment: '2 huollettavaa'
  },
  {
    ssn: '311299-999E',
    commonName: 'Ville',
    givenName: 'Ville',
    surname: 'Vilkas',
    comment: '1 huollettavaa'
  },
  {
    ssn: '270372-905L',
    commonName: 'Sirkka-Liisa Marja-Leena Minna-Mari Anna-Kaisa',
    givenName: 'Sirkka-Liisa',
    surname: 'Korhonen-Hämäläinen',
    comment: '2 huollettavaa'
  },
  {
    ssn: '280674-990X',
    commonName: 'Kaarina Helena Annikki Anneli',
    givenName: 'Kaarina',
    surname: 'Honkilahti',
    comment: '2 huollettavaa'
  },
  {
    ssn: '150794-9463',
    commonName: 'Hannele Johanna Kaarina Anneli',
    givenName: 'Hannele',
    surname: 'Hattula',
    comment: '2 huollettavaa'
  },
  {
    ssn: '290385-9900',
    commonName: 'Anna Maria Helena Sofia',
    givenName: 'Anna',
    surname: 'Haaga',
    comment: '2 huollettavaa'
  },
  {
    ssn: '170590-9540',
    commonName: 'Kaarina Marjatta Anna Liisa',
    givenName: 'Kaarina',
    surname: 'Högfors',
    comment: '2 huollettavaa'
  },
  {
    ssn: '010180-1232',
    commonName: 'John',
    givenName: 'John',
    surname: 'Doe',
    comment: '2 huollettavaa'
  },
  {
    ssn: '010279-123L',
    commonName: 'Joan',
    givenName: 'Joan',
    surname: 'Doe',
    comment: '1 huollettavaa'
  },
  {
    ssn: '271170-917X',
    commonName: 'Kaarina Liisa Annikki Sofia',
    givenName: 'Kaarina',
    surname: 'Heinjoki',
    comment: '2 huollettavaa'
  },
  {
    ssn: '020474-9187',
    commonName: 'Terttu Sylvi Sofia',
    givenName: 'Terttu',
    surname: 'Kankaanperä',
    comment: '2 huollettavaa'
  },
  {
    ssn: '311288A999L',
    commonName: '',
    givenName: '',
    surname: '',
    comment: '1 huollettavaa'
  },
  {
    ssn: '020898-945H',
    commonName: 'Eemeli Mikael Tapio',
    givenName: 'Eemeli',
    surname: 'Muurinen',
    comment: '2 huollettavaa'
  },
  {
    ssn: '130894-917N',
    commonName: 'Helena Kaarina Anna Johanna',
    givenName: 'Helena',
    surname: 'Hattula',
    comment: '2 huollettavaa'
  },
  {
    ssn: '231182-9661',
    commonName: 'Anna Marjatta Helena Anneli',
    givenName: 'Anna',
    surname: 'Kalanti',
    comment: '2 huollettavaa'
  },
  {
    ssn: '210593-9430',
    commonName: 'Maria Helena Marjatta',
    givenName: 'Maria',
    surname: 'Jaala',
    comment: '2 huollettavaa'
  },
  {
    ssn: '300190-9257',
    commonName: 'Heikki Tapio Eemeli',
    givenName: 'Heikki',
    surname: 'Haahka',
    comment: '2 huollettavaa'
  },
  {
    ssn: '130973-9825',
    commonName: 'Hannele Johanna Anneli Hannele',
    givenName: 'Hannele',
    surname: 'Haga',
    comment: '2 huollettavaa'
  },
  {
    ssn: '010101-123N',
    commonName: 'Teemu Taneli Tapio',
    givenName: 'Teemu',
    surname: 'Testaaja',
    comment: '2 huollettavaa'
  },
  {
    ssn: '010882-983Y',
    commonName: 'Matti Tapani Antero Mikael',
    givenName: 'Matti',
    surname: 'Karjala',
    comment: '2 huollettavaa'
  },
  {
    ssn: '120220A995L',
    commonName: 'Hillary',
    givenName: 'Hillary',
    surname: 'Foo',
    comment: '0 huollettavaa'
  },
  {
    ssn: '220281-9456',
    commonName: 'Mikael Ilmari Juhani Johannes',
    givenName: 'Mikael',
    surname: 'Högfors',
    comment: '2 huollettavaa'
  },
  {
    ssn: '150978-9025',
    commonName: 'Sirpa Maria Annikki',
    givenName: 'Sirpa',
    surname: 'Silkkiuikku',
    comment: '2 huollettavaa'
  },
  {
    ssn: '071082-9435',
    commonName: 'Tapio Tapani Kalevi Matti',
    givenName: 'Tapio',
    surname: 'Heinjoki',
    comment: '2 huollettavaa'
  },
  {
    ssn: '221071-9131',
    commonName: 'Tapani Matti Kalevi Olavi',
    givenName: 'Tapani',
    surname: 'Joensuu',
    comment: '2 huollettavaa'
  },
  {
    ssn: '010170-960F',
    commonName: 'Anna Kaarina Marjatta',
    givenName: 'Anna',
    surname: 'Demo',
    comment: '2 huollettavaa'
  },
  {
    ssn: '120482-955X',
    commonName: 'Tauno Tapani Kalevi',
    givenName: 'Tauno',
    surname: 'Tammi',
    comment: '2 huollettavaa'
  },
  {
    ssn: '090275-9724',
    commonName: 'Helena Sofia Johanna',
    givenName: 'Helena',
    surname: 'Jokioinen',
    comment: '2 huollettavaa'
  },
  {
    ssn: '060195-966B',
    commonName: 'Hannele Kaarina Marjatta',
    givenName: 'Hannele',
    surname: 'Finström',
    comment: '2 huollettavaa, palveluseteliyksikön johtaja'
  },
  {
    ssn: '050482-9741',
    commonName: 'Tapani Kalevi Tapio Antero',
    givenName: 'Tapani',
    surname: 'Alajärvi',
    comment: '2 huollettavaa'
  },
  {
    ssn: '070644-937X',
    commonName: 'Johannes Olavi Antero Tapio',
    givenName: 'Johannes',
    surname: 'Karhula',
    comment: '3 huollettavaa'
  },
  {
    ssn: '210281-9988',
    commonName: 'Nordea Nalle Arne',
    givenName: 'Nordea',
    surname: 'Demo',
    comment: '2 huollettavaa'
  },
  {
    ssn: '150288-971A',
    commonName: 'Kaarina Helena Marjatta',
    givenName: 'Kaarina',
    surname: 'Jurva',
    comment: '2 huollettavaa'
  },
  {
    ssn: '290393-9913',
    commonName: 'Matti Olavi Antero',
    givenName: 'Matti',
    surname: 'Finström',
    comment: '2 huollettavaa'
  },
  {
    ssn: '100774-9306',
    commonName: 'Helena Anna Maria',
    givenName: 'Helena',
    surname: 'Joutsa',
    comment: '2 huollettavaa'
  },
  {
    ssn: '100373-9733',
    commonName: 'Olavi Tapio Johannes',
    givenName: 'Olavi',
    surname: 'Haaga',
    comment: '2 huollettavaa'
  },
  {
    ssn: '290377-9377',
    commonName: 'Annikki Maria Anna',
    givenName: 'Annikki',
    surname: 'Eno',
    comment: '2 huollettavaa'
  },
  {
    ssn: '031083-910S',
    commonName: 'Seija Anna Kaarina',
    givenName: 'Seija',
    surname: 'Sotka',
    comment: '2 huollettavaa'
  },
  {
    ssn: '260888-990V',
    commonName: 'Liisa Anna Kaarina Marjatta',
    givenName: 'Liisa',
    surname: 'Aspö',
    comment: '2 huollettavaa'
  },
  {
    ssn: '170595-9151',
    commonName: 'Ahto Olavi Antero',
    givenName: 'Ahto',
    surname: 'Simakuutio',
    comment: '2 huollettavaa'
  },
  {
    ssn: '010495-965H',
    commonName: 'Marjatta Liisa Annikki',
    givenName: 'Marjatta',
    surname: 'Anttola',
    comment: '2 huollettavaa'
  },
  {
    ssn: '150580-999K',
    commonName: 'Anelma',
    givenName: 'Anelma',
    surname: 'Aapinen',
    comment: '1 huollettavaa'
  },
  {
    ssn: '020394-958V',
    commonName: 'Mikael Matti Ilmari',
    givenName: 'Mikael',
    surname: 'Anttola',
    comment: '2 huollettavaa'
  },
  {
    ssn: '210586-987L',
    commonName: 'Anneli Helena Johanna Kaarina',
    givenName: 'Anneli',
    surname: 'Aspö',
    comment: '2 huollettavaa'
  },
  {
    ssn: '081181-9984',
    commonName: 'Sylvi Liisa Sofia',
    givenName: 'Sylvi',
    surname: 'Marttila',
    comment: '2 huollettavaa'
  }
]
