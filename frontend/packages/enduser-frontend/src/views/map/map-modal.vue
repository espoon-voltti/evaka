<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <a href="#" @click.prevent="open">
      <font-awesome-icon :icon="['fal', 'map-marker-alt']"></font-awesome-icon>
      Valitse kartalta
    </a>
    <modal v-show="showModal" class="map-modal" @modal-open="fitBounds">
      <h3 slot="header" class="confirm-header">
        <slot name="header">Valitse kartalta</slot>
      </h3>
      <!-- Body -->
      <div slot="body">
        <gmap-map
          :center="center"
          :zoom="11"
          :options="mapOptions"
          ref="modalMap"
        >
          <gmap-info-window
            v-if="infoWinUnit"
            :id="infoWinID"
            :position="infoWinPos"
            :options="infoWinOptions"
            :opened="infoWinOpen"
            @closeclick="closeInfoWin()"
          >
            <div class="unit-details">
              <h4 class="unit-name">{{ infoWinUnit.name }}</h4>
              <span class="unit-address">
                {{ infoWinUnit.address }}, {{ infoWinUnit.postalCode }}
                {{ 'Espoo' /*@todo translations */ }}
              </span>
              <div class="groups-container">
                <div
                  :key="group.id"
                  v-for="group in infoWinUnit.groups"
                  :class="['group', { disabled: group.$isDisabled }]"
                  @click.prevent="onGroupSelected(group)"
                >
                  <span class="strong description">{{
                    group.description
                  }}</span>
                  <span class="schedule">{{ group.schedule }}</span>
                  <span v-show="!group.$isDisabled" class="btn-add-group"
                    >Lisää</span
                  >
                </div>
              </div>
            </div>
          </gmap-info-window>

          <gmap-marker
            v-for="m in markers"
            :key="m.id"
            :position.sync="m.position"
            :clickable="true"
            :draggable="false"
            :icon="markerIconUrl(m, mapSettings.icons.otherMarkers.scaledSize)"
            :travelMode="false"
            @click="showInfoWindow(m), (center = m.position)"
          >
          </gmap-marker>
        </gmap-map>
      </div>
      <!-- Footer -->
      <div slot="footer" class="modal-footer field is-grouped buttons">
        <!-- Cancel button -->
        <button
          class="btn-reject button is-link"
          taborder="0"
          @click="onCancel"
        >
          Peruuta
        </button>
      </div>
    </modal>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { config } from '@evaka/enduser-frontend/src/config'
  import * as types from '@/store/mutation-types'
  import { toMarker } from '@/store/modules/units'
  import mapSettings from '@/views/map/map-settings.ts'
  import Modal from '@/components/modal/base-modal.vue'

  export default {
    components: { Modal },
    props: ['units'],
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
        zIndex: 999,
        gridSize: 20,
        maxZoom: 13,
        showModal: false
      }
    },
    computed: {
      ...mapGetters(['mapApiLoaded']),
      mapOptions() {
        return {
          ...mapSettings.mapOptions,
          streetViewControl: false
        }
      },
      markers() {
        return this.units.filter((unit) => unit.location != null).map(toMarker)
      }
    },
    mounted() {
      this.$refs.modalMap.$gmapApiPromiseLazy().then(() => {
        this.$store.commit(types.MAP_API_LOADED)
      })
    },
    methods: {
      fitBounds() {
        if (this.mapApiLoaded && this.markers.length && !this.destination) {
          const bounds = new google.maps.LatLngBounds()
          this.markers.forEach((marker) => bounds.extend(marker.position))

          // Adjust bounds if there is only 1 marker => map stays at a reasonable zoom level
          if (this.markers.length === 1) {
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

          this.$refs.modalMap.fitBounds(bounds)
        }
      },
      markerIconUrl(marker, markerSize) {
        if (marker.services.length === 1) {
          return {
            // NOTE: Due to Webpack's way to handling imports, asset directories must be hard-coded into require statements
            url: require('@evaka/enduser-frontend/src/assets/' + marker.services[0] + '.png'),
            scaledSize: markerSize
          }
        } else if (marker.services.length > 1) {
          return {
            url: require('@evaka/enduser-frontend/src/assets/markerMULTIPLE.png'),
            scaledSize: markerSize
          }
        } else {
          return {
            url: require('@evaka/enduser-frontend/src/assets/marker.png'),
            scaledSize: markerSize
          }
        }
      },
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
      closeInfoWin() {
        this.infoWinOpen = false
        this.infoWinID = ''
        this.infoWinUnit = null
      },
      onGroupSelected(group) {
        this.$emit('selected', group)
        this.close()
      },
      onCancel() {
        this.close()
      },
      open() {
        this.showModal = true
      },
      close() {
        this.closeInfoWin()
        this.showModal = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .modal-footer {
    justify-content: flex-end;
  }

  .vue-map {
    .unit-details {
      position: relative;
      small {
        font-size: 12px;
      }
    }
  }

  .vue-map-container {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
  }
</style>

<style lang="scss">
  .map-modal {
    .modal-container {
      position: relative;
      height: 95vh;
      width: 95vw;
      max-height: 600px;
      max-width: 600px;
      display: flex;
      flex-direction: column;
    }

    .modal-body {
      flex: 1;
      position: relative;
    }
  }

  img.icon {
    -webkit-transform-style: preserve-3d;
    transform-style: preserve-3d;
  }
</style>

<style lang="scss" scoped>
  .unit-name {
    font-weight: 500;
    font-size: 1rem;
  }

  .unit-address {
  }

  .groups-container {
    margin-top: 0.5rem;

    .group {
      padding: 0.5rem 1rem;
      border: 1px solid lightgrey;
      margin-bottom: 0.325rem;
      text-align: left;

      .description {
        margin-right: 1rem;
      }
      .schedule {
        margin-right: 1rem;
      }

      .btn-add-group {
        font-size: 0.825rem;
        line-height: 1rem;
        font-weight: 600;
        color: #00d1b2;
        float: right;
      }

      &:hover:not(.disabled) {
        border-color: rgba(#00d1b2, 0.9);
      }

      &.disabled {
        color: lightgrey;
        border-color: lightgrey;
      }
    }
  }
</style>
