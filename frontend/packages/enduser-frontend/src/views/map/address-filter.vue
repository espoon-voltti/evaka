<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <section>
    <div class="search-group">
      <label>{{ $t('map-filters.address') }}:</label>
      <location-picker
        :addresses="autocompletedAddresses"
        selectAction="selectLocation"
        searchAction="searchAddresses"
        v-model="selectedLocationsModel"
      >
      </location-picker>

      <slot></slot>
    </div>

    <transition name="fade">
      <div
        v-if="selectedLocation && !hideDistance"
        class="search-group distance-slider"
      >
        <label class="distance-label">
          Etäisyys osoitteesta:
        </label>
        <input
          class="distance-slider-input"
          :disabled="!hasSelectedLocation"
          type="range"
          min="1"
          max="20"
          step="1"
          v-model="distance"
        />
        <span class="distance">{{ distance }} km</span>
      </div>
    </transition>
  </section>
</template>

<script>
  import LocationPicker from '@/views/map/location-picker.vue'
  import { mapGetters } from 'vuex'
  import { config } from '@evaka/enduser-frontend/src/config'

  export default {
    data() {
      return {
        distance: config.filters.addressDefaultDistance,
        selectedLocationsModel: null
      }
    },
    components: {
      LocationPicker
    },
    props: {
      hideDistance: {
        type: Boolean
      }
    },
    computed: {
      ...mapGetters(['autocompletedAddresses', 'selectedLocation']),
      hasSelectedLocation() {
        return this.selectedLocation !== null
      }
    },
    methods: {
      filterChanged() {
        if (!this.hasSelectedLocation) {
          this.$emit('addressFilterCleared')
          this.selectedLocationsModel = null
        } else {
          this.$emit('addressFilterChanged', {
            distance: this.distance * 1000,
            lat: this.selectedLocation.lat,
            lng: this.selectedLocation.lng,
            address: this.selectedLocationsModel.address
          })
        }
      }
    },
    watch: {
      selectedLocation() {
        this.filterChanged()
      },
      distance() {
        this.filterChanged()
      }
    }
  }
</script>

<!--suppress HtmlUnknownAttribute -->
<style lang="scss" scoped>
  input[type='range'] {
    padding: 0;
    &::-ms-track {
      height: 4px;
      cursor: pointer;
      background: transparent;
      border-color: transparent;
      border-width: 8px 0;
      color: transparent;
    }
    &::-ms-fill-lower {
      background: #dcdcdc;
      border-radius: 5px;
      box-shadow: 1px 1px 1px rgba(0, 0, 0, 0), 0 0 1px rgba(0, 0, 0, 0);
    }
    &::-ms-fill-upper {
      background: #dcdcdc;
      border-radius: 5px;
      box-shadow: 1px 1px 1px rgba(0, 0, 0, 0), 0 0 1px rgba(0, 0, 0, 0);
    }
    &::-ms-thumb {
      border: 1px solid;
      height: 12px;
      width: 12px;
      border-radius: 50%;
      background: #ffffff;
      cursor: pointer;
    }
    &::-ms-tooltip {
      display: none;
    }
  }

  .distance-label {
    display: block;
    margin-bottom: 0.325rem;
  }

  $distance-handle-color: #6a94a5 !default;
  $distance-handle-color-hover: #3273dc !default;
  $distance-handle-size: 20px !default;

  $distance-track-color: #e4e4e4 !default;
  $distance-track-height: 10px !default;

  $distance-label-width: 60px !default;

  .distance-slider {
    display: flex;
    flex-wrap: wrap;
    align-items: center;

    .distance-label {
      flex: 100% 0 0;
    }

    .distance-slider-input {
      -webkit-appearance: none;
      flex: 1 0 auto;
      height: $distance-track-height;
      border-radius: 5px;
      background: $distance-track-color;
      padding: 0;
      margin: 0;
      vertical-align: middle;

      // Range Handle
      &::-webkit-slider-thumb {
        appearance: none;
        width: $distance-handle-size;
        height: $distance-handle-size;
        border-radius: 50%;
        background: $distance-handle-color;
        cursor: pointer;
        transition: background 0.15s ease-in-out;

        &:hover {
          background: $distance-handle-color-hover;
        }
      }

      &:active::-webkit-slider-thumb {
        background: $distance-handle-color-hover;
      }

      &::-moz-range-thumb {
        width: $distance-handle-size;
        height: $distance-handle-size;
        border: 0;
        border-radius: 50%;
        background: $distance-handle-color;
        cursor: pointer;
        transition: background 0.15s ease-in-out;

        &:hover {
          background: $distance-handle-color-hover;
        }
      }

      &:active::-moz-range-thumb {
        background: $distance-handle-color-hover;
      }
    }

    .distance {
      font-weight: 600;
      margin-left: 0.5rem;
    }
  }
</style>
