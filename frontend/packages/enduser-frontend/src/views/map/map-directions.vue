<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template></template>

<script>
  import * as VueGoogleMap from 'vue2-google-maps'

  export default {
    props: {
      origin: {
        type: Object,
        default: null
      },
      destination: Object,
      directionsDisplay: Object,
      travelMode: String
    },
    data() {
      return {
        directionsService: null,
        marker: null,
        markerCircle: {
          path: google.maps.SymbolPath.CIRCLE,
          fillColor: 'white',
          fillOpacity: 0.75,
          scale: 4.5,
          strokeColor: 'navyblue',
          strokeWeight: 2,
          strokeOpacity: 0.65
        }
      }
    },
    computed: {
      markerA() {
        if (this.origin) {
          return this.origin
        }
      },
      markerB() {
        if (this.destination) {
          return this.destination
        }
      }
    },
    watch: {
      travelMode() {
        this.calcRoute(
          this.directionsService,
          this.directionsDisplay,
          this.travelMode
        )
      },
      destination() {
        this.calcRoute(
          this.directionsService,
          this.directionsDisplay,
          this.travelMode
        )
      }
    },
    methods: {
      createMarker(loc, map) {
        this.marker = new google.maps.Marker({
          position: loc,
          icon: this.markerCircle,
          map,
          zIndex: google.maps.Marker.MAX_ZINDEX + 1
        })
      },
      calcRoute(service, display, travelMode) {
        const request = {
          origin: this.markerA,
          destination: this.markerB,
          travelMode,
          provideRouteAlternatives: true
        }
        service.route(request, (response, status) => {
          if (status === 'OK') {
            if (this.marker) {
              this.removeMarkers()
            }
            this.createMarker(this.markerB, this.$map)

            display.setDirections(response)
            const directionsDistance =
              Math.round(
                display.getDirections().routes[display.getRouteIndex()].legs[0]
                  .distance.value / 100
              ) / 10
            this.$emit('directionsDistance', directionsDistance)
          }
        })
      },
      clearDirections() {
        this.directionsDisplay.setMap(null)
      },
      removeMarkers() {
        this.marker.setMap(null)
      }
    },
    mixins: [VueGoogleMap.MapElementMixin],
    beforeDestroy() {
      this.clearDirections()
    },
    deferredReady() {
      const Gmap = this.$map
      this.directionsService = new google.maps.DirectionsService()

      if (this.markerA) {
        this.directionsDisplay.setMap(Gmap)
        this.calcRoute(
          this.directionsService,
          this.directionsDisplay,
          this.travelMode
        )
      }
    }
  }
</script>
