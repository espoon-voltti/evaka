// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import type { CitizenCustomizations } from 'lib-customizations/types'

import { citizenConfig } from './appConfigs'
import EspooLogo from './assets/EspooLogoPrimary.svg'
import featureFlags from './featureFlags'
import mapConfig from './mapConfig'

const MultiLineCheckboxLabel = styled(FixedSpaceColumn).attrs({
  spacing: 'zero'
})`
  margin-top: -6px;
`

const customizations: CitizenCustomizations = {
  appConfig: citizenConfig,
  langs: ['fi', 'sv', 'en'],
  translations: {
    fi: {
      applications: {
        editor: {
          serviceNeed: {
            assistanceNeeded:
              'Lapsella on kehitykseen tai oppimiseen liittyvä tuen tarve'
          },
          verification: {
            serviceNeed: {
              assistanceNeed: {
                assistanceNeed: 'Kehitykseen tai oppimiseen liittyvä tuen tarve'
              }
            }
          }
        }
      },
      income: {
        assure: (
          <MultiLineCheckboxLabel>
            <span>
              Vakuutan antamani tiedot oikeiksi ja olen tutustunut
              asiakasmaksutiedotteeseen:
            </span>
            <a
              href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-asiakasmaksut#section-59617"
              target="_blank"
              rel="noreferrer"
            >
              Lasten varhaiskasvatuksesta perittävät maksut 1.8.2023 alkaen |
              Espoon kaupunki
            </a>
          </MultiLineCheckboxLabel>
        ),
        incomeType: {
          description: (
            <>
              Jos olet yrittäjä, mutta sinulla on myös muita tuloja, valitse
              sekä <strong>Yrittäjän tulotiedot</strong>, että{' '}
              <strong>Asiakasmaksun määritteleminen tulojen mukaan</strong>.
            </>
          ),
          grossIncome: 'Maksun määritteleminen tulojen mukaan'
        },
        grossIncome: {
          title: 'Tulotietojen täyttäminen',
          estimate: 'Arvio palkkatuloistani (ennen veroja)'
        }
      }
    },
    sv: {
      applications: {
        editor: {
          serviceNeed: {
            assistanceNeeded:
              'Barnet har behov av stöd i anslutning till utveckling eller lärande'
          },
          verification: {
            serviceNeed: {
              assistanceNeed: {
                assistanceNeed:
                  'Behov av stöd i anslutning till utveckling eller lärande'
              }
            }
          }
        }
      },
      income: {
        assure: (
          <MultiLineCheckboxLabel>
            <span>
              Jag försäkrar att de uppgifter jag lämnat in är riktiga och jag
              har bekantat mig med kundcirkuläret gällande avgifter för
              småbarnspedagogik:{' '}
              <a
                href="https://www.espoo.fi/sv/artiklar/avgifter-smabarnspedagogik-fran-182023"
                target="_blank"
                rel="noreferrer"
              >
                Avgifter för småbarnspedagogik från 1.8.2023 | Esbo stad
                (espoo.fi)
              </a>
            </span>
          </MultiLineCheckboxLabel>
        ),
        incomeType: {
          description: (
            <>
              Om du är företagare men har också andra inkomster, välj både{' '}
              <strong>Företagarens inkomstuppgifter</strong>, och{' '}
              <strong>Fastställande av klientavgiften enligt inkomster</strong>.
            </>
          ),
          grossIncome: 'Fastställande av avgiften enligt inkomster'
        },
        grossIncome: {
          title: 'Att fylla i uppgifterna om inkomster',
          estimate: 'Uppskattning av mina bruttolön (före skatt)'
        }
      }
    },
    en: {
      applications: {
        editor: {
          serviceNeed: {
            assistanceNeeded:
              'The child needs support for development or learning'
          },
          verification: {
            serviceNeed: {
              assistanceNeed: {
                assistanceNeed: 'Support for development or learning'
              }
            }
          }
        }
      },
      income: {
        assure: (
          <MultiLineCheckboxLabel>
            <span>
              I confirm that the information I have provided is accurate, and I
              have reviewed the customer fee information:
            </span>
            <a
              href="https://www.espoo.fi/en/articles/early-education-fees-1-august-2023"
              target="_blank"
              rel="noreferrer"
            >
              Early education fees as of 1 August 2023 | City of Espoo
            </a>
          </MultiLineCheckboxLabel>
        ),
        incomeType: {
          description: (
            <>
              If you are an entrepreneur but also have other income, choose both{' '}
              <strong>Entrepreneur&apos;s income information</strong>, and{' '}
              <strong>Determination of the client fee by income</strong>.
            </>
          ),
          grossIncome: 'Determination of the client fee by income'
        },
        grossIncome: {
          title: 'Filling in income data',
          estimate: 'Estimate of my gross salary (before taxes)'
        }
      }
    }
  },
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
  routeLinkRootUrl: 'https://reittiopas.hsl.fi/reitti/',
  mapConfig,
  featureFlags,
  getMaxPreferredUnits() {
    return 3
  }
}

export default customizations
