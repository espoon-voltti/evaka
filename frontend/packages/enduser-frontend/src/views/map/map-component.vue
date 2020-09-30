<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <gmap-map
    :center="center"
    :zoom="11"
    :options="mapSettings.mapOptions"
    ref="serviceMap"
  >
    <gmap-info-window
      v-if="infoWinUnit"
      :id="infoWinID"
      :position="infoWinPos"
      :options="infoWinOptions"
      :opened="infoWinOpen"
      @closeclick="closeInfoWin()"
    >
      <unit-details
        :unit="infoWinUnit"
        @routeTo="onRouteTo"
      >
      </unit-details>
    </gmap-info-window>

    <gmap-marker
      v-if="address != null"
      :position.sync="address"
      :clickable="false"
      :draggable="false"
      :icon="mapSettings.icons.homeMarker"
      :z-index="zIndex"
    >
    </gmap-marker>

    <gmap-marker
      v-for="m in markersList"
      :key="m.id"
      :position.sync="m.position"
      :clickable="true"
      :draggable="false"
      :icon="markerIconUrl(m, mapSettings.icons.otherMarkers.scaledSize)"
      :travelMode="travelMode"
      @click="showInfoWindow(m), (center = m.position)"
    >
    </gmap-marker>

    <map-directions
      v-if="markers && address && destination"
      :origin="address"
      :destination="destination"
      :directionsDisplay="directionsDisplay"
      :travelMode="travelMode"
      @directionsDistance="onDirectionsDistance"
    >
    </map-directions>
  </gmap-map>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { config } from '@evaka/enduser-frontend/src/config'
  import UnitDetails from '@/views/map/unit-details.vue'
  import MapDirections from '@/views/map/map-directions.vue'
  // Map options in separate file
  import mapSettings from '@/views/map/map-settings'
  import * as types from '@/store/mutation-types'
  import _ from 'lodash'
  import { UNIT_TYPE } from '@/constants'

  export default {
    props: [
      'markers',
      'units',
      'address',
      'destination',
      'travelMode'
    ],
    components: {
      UnitDetails,
      MapDirections
    },
    data() {
      return {
        mapSettings,
        center: config.maps.espooCoordinates,
        infoWinID: '',
        infoWinUnit: null,
        infoWinPos: { lat: 0, lng: 0 },
        infoWinOptions: {
          pixelOffset: { width: 0, height: -35 }
        },
        infoWinOpen: false,
        directionsDisplay: null,
        zIndex: 999,
        gridSize: 20,
        maxZoom: 13
      }
    },
    watch: {
      markersList() {
        this.fitBounds()
      },
      mapApiLoaded() {
        this.fitBounds()
        this.directionsDisplay = new google.maps.DirectionsRenderer({
          suppressMarkers: true
        })
      },
      units() {
        // Info window unit model has to be updated if units are updated
        if (this.infoWinOpen && this.infoWinUnit) {
          this.infoWinUnit = this.units.find(
            (unit) => unit.id === this.infoWinUnit.id
          )
        }
      }
    },
    methods: {
      showInfoWindow(marker) {
        this.infoWinPos = { lat: marker.position.lat, lng: marker.position.lng }
        this.infoWinUnit = this.units.find((unit) => unit.id === marker.id)

        // same -> toggle between visibility
        if (this.infoWinID === this.infoWinUnit.id) {
          this.infoWinOpen = !this.infoWinOpen
          this.infoWinID = this.infoWinUnit.id
          // not same -> open the new one
        } else {
          this.infoWinOpen = true
          this.infoWinID = this.infoWinUnit.id
        }
      },
      // close info window when default 'x' is clicked
      closeInfoWin() {
        this.infoWinOpen = false
      },
      fitBounds() {
        // google maps api has to be loaded before LatLngBounds can be created
        if (this.mapApiLoaded && this.markersList.length && !this.destination) {
          const bounds = new google.maps.LatLngBounds()
          this.markers.forEach((marker) => bounds.extend(marker.position))

          if (this.address !== null) {
            bounds.extend(this.address)
          }

          // Adjust bounds if there is only 1 marker => map stays at a reasonable zoom level
          if (this.markersList.length === 1) {
            if (bounds.getNorthEast().equals(bounds.getSouthWest())) {
              const extendPoint1 = new google.maps.LatLng(
                bounds.getNorthEast().lat() + 0.005,
                bounds.getNorthEast().lng() + 0.005
              )
              const extendPoint2 = new google.maps.LatLng(
                bounds.getNorthEast().lat() - 0.005,
                bounds.getNorthEast().lng() - 0.005
              )
              bounds.extend(extendPoint1)
              bounds.extend(extendPoint2)
            }
          }

          this.$refs.serviceMap.fitBounds(bounds)
        }
      },
      onRouteTo(unit) {
        this.$emit('routeTo', unit)
      },
      onDirectionsDistance(distance) {
        this.$emit('onDirectionsDistance', distance)
      },
      markerIconUrl(marker, markerSize) {
        let iconUrl = ''
        if (marker.services.includes(UNIT_TYPE.PRESCHOOL)) {
          // NOTE: Due to Webpack's way to handling imports, asset directories must be hard-coded into require statements
          iconUrl = require('@evaka/enduser-frontend/src/assets/markerPRESCHOOL.png')
        } else {
          iconUrl =
            require('@evaka/enduser-frontend/src/assets/' +
            'marker' +
            (marker.services.length === 1 ? marker.services[0] : '') +
            '.png')
        }
        return {
          url: iconUrl,
          scaledSize: markerSize
        }
      },
      checkIfDuplicateLocation(array, marker) {
        return (
          _.findIndex(array, {
            position: { lat: marker.position.lat, lng: marker.position.lng }
          }) !== -1
        )
      }
    },
    computed: {
      ...mapGetters(['mapApiLoaded']),
      markersList() {
        const markers = _.cloneDeep(this.markers)
        let startlng = 0
        const checkedMarkers = []

        const mutatedMarkers = markers.map((marker) => {
          if (this.checkIfDuplicateLocation(checkedMarkers, marker)) {
            startlng += 0.00008
            marker.position.lng += startlng
          } else {
            startlng = 0
          }
          checkedMarkers.push(marker)
          return marker
        })

        return mutatedMarkers
      }
    },
    mounted() {
      this.$refs.serviceMap.$gmapApiPromiseLazy().then(() => {
        this.$store.commit(types.MAP_API_LOADED)
        this.mapSettings.icons = {
          homeMarker: {
            scaledSize: new google.maps.Size(42, 42),
            url: require('@evaka/enduser-frontend/src/assets/markerHOME.png')
          },
          otherMarkers: {
            scaledSize: new google.maps.Size(42, 42)
          }
        }

        // DirectionsRenderer has to be initialized also here if map component is opened the second time
        // (it cannot be moved to store because of maximum call stack size error)
        this.directionsDisplay = new google.maps.DirectionsRenderer({
          suppressMarkers: true
        })
      })
    }
  }
</script>

<style lang="scss" scoped>
  .vue-map-container {
    height: 100%;
    min-height: 540px;
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
  }
  .vue-map {
    height: 100%;
  }
</style>

<style>
  img.icon {
    -webkit-transform-style: preserve-3d;
    transform-style: preserve-3d;
  }
</style>
