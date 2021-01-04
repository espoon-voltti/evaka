// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defaultsDeep } from 'lodash'
import { BaseAppConfig, DeepReadonly } from '@evaka/lib-common/src/types'
import { getEnvironment } from '@evaka/lib-common/src/utils/helpers'

type AppConfig = DeepReadonly<
  BaseAppConfig & {
    api: {
      areas: string
      geocoding: string
      autocomplete: string
      applications: string
    }
    application: {
      saveIntervalMs: number
    }
    bearerToken: string
    filters: {
      addressDefaultDistance: number
      addressMinimumAutocompleteInputLength: number
      autocompleteDebounceWaitMs: number
    }
    maps: {
      googleApiKey: string
      espooCoordinates: {
        lat: number
        lng: number
      }
      styles: object[]
    }
    feature: {
      daycareApplication: boolean
      preschoolApplication: boolean
      selectApplicationType: boolean
      attachments: boolean
      citizenFrontend: boolean
    }
  }
>
const configs: Record<string, AppConfig> = {}
configs._default = {
  api: {
    areas: '/api/application/public/areas',
    geocoding: '/api/application/geocode',
    autocomplete: '/api/application/autocomplete',
    applications: '/api/application/application'
  },

  application: {
    saveIntervalMs: 60000
  },

  // TEMPORARY, will be replaced with the real bearer token
  bearerToken:
    'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1MjYxNjE4ODgsImV4cCI6MTU1NzY5Nzg4OCwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoiN2ExYzEyYjQtNzJjMS00YzIwLTk2M2MtYmI0YTc5NDM5OTlmIn0.FqOV5Ui_V7hYEYAPJX4ZzpNjXkAghnYaHSzAHkR7LSI',

  filters: {
    addressDefaultDistance: 3,
    addressMinimumAutocompleteInputLength: 3,
    autocompleteDebounceWaitMs: 500
  },

  maps: {
    googleApiKey: 'AIzaSyDsQ-afaWGiGbIUd9JMmdknJ9zuAvXDv1k',
    espooCoordinates: { lat: 60.2051256, lng: 24.6541313 },
    styles: [
      {
        featureType: 'all',
        elementType: 'labels.text.fill',
        stylers: [{ color: '#736c68' }]
      },
      {
        featureType: 'landscape.man_made',
        elementType: 'geometry.fill',
        stylers: [{ color: '#e7e6e5' }]
      },
      {
        featureType: 'landscape.natural',
        elementType: 'all',
        stylers: [{ visibility: 'on' }, { color: '#d4e4d3' }]
      },
      {
        featureType: 'landscape.natural',
        elementType: 'labels.text.fill',
        stylers: [{ color: '#55795b' }]
      },
      {
        featureType: 'landscape.natural',
        elementType: 'labels.text.stroke',
        stylers: [{ visibility: 'on' }, { color: '#ebebeb' }]
      },
      {
        featureType: 'poi',
        elementType: 'geometry.fill',
        stylers: [{ visibility: 'on' }, { color: '#f5f5f5' }]
      },
      {
        featureType: 'poi.attraction',
        elementType: 'labels',
        stylers: [{ visibility: 'on' }]
      },
      {
        featureType: 'poi.business',
        elementType: 'all',
        stylers: [{ visibility: 'off' }]
      },
      {
        featureType: 'poi.business',
        elementType: 'labels.text',
        stylers: [{ visibility: 'off' }]
      },
      {
        featureType: 'poi.government',
        elementType: 'labels',
        stylers: [{ visibility: 'on' }]
      },
      {
        featureType: 'poi.medical',
        elementType: 'labels',
        stylers: [{ visibility: 'on' }]
      },
      {
        featureType: 'poi.park',
        elementType: 'geometry.fill',
        stylers: [{ color: '#d4e4d3' }]
      },
      {
        featureType: 'poi.school',
        elementType: 'all',
        stylers: [{ visibility: 'on' }]
      },
      {
        featureType: 'poi.sports_complex',
        elementType: 'labels',
        stylers: [{ visibility: 'off' }]
      },
      {
        featureType: 'road',
        elementType: 'geometry.stroke',
        stylers: [{ color: '#e7e6e5' }, { gamma: '0.65' }, { lightness: '0' }]
      },
      {
        featureType: 'transit',
        elementType: 'all',
        stylers: [{ visibility: 'on' }]
      },
      {
        featureType: 'transit',
        elementType: 'geometry',
        stylers: [{ weight: '1' }, { color: '#c3c3c3' }]
      },
      {
        featureType: 'transit',
        elementType: 'labels.text',
        stylers: [{ visibility: 'on' }]
      },
      {
        featureType: 'water',
        elementType: 'all',
        stylers: [{ color: '#88c0e3' }]
      },
      {
        featureType: 'water',
        elementType: 'labels',
        stylers: [{ visibility: 'off' }]
      }
    ]
  },

  sentry: {
    dsn: 'https://dbab5ba9b7d94c87bc4d91bee8756e74@sentry.io/1820307',
    enabled: false
  },

  feature: {
    daycareApplication: true,
    preschoolApplication: true,
    selectApplicationType: true,
    attachments: true,
    citizenFrontend: true
  }
}
configs.dev = defaultsDeep(
  {
    maps: {
      googleApiKey: 'AIzaSyCo8b3pEGS4IvVOBHExYePR7ru1SoHtKOs'
    }
  },
  configs._default
)
configs.test = defaultsDeep(
  {
    maps: {
      googleApiKey: 'AIzaSyAhPTjH3-vK8U6RJblEabS7JJYNgZmjzV8'
    }
  },
  configs._default
)
configs.staging = defaultsDeep(
  {
    maps: {
      googleApiKey: 'AIzaSyBiPAcmG0YW6dfMQhXCePRh3YUgfVFJ8d8'
    },
    sentry: {
      enabled: true
    }
  },
  configs._default
)
configs.prod = defaultsDeep(
  {
    maps: {
      googleApiKey: 'AIzaSyAfTS8vvpRjEoKfY_5qNT8V4a8swBv-p6k'
    },
    sentry: {
      enabled: true
    },
    feature: {
      daycareApplication: true,
      preschoolApplication: true,
      selectApplicationType: true,
      attachments: true,
      citizenFrontend: false
    }
  },
  configs._default
)

export const config: AppConfig = configs[getEnvironment()] || configs._default
