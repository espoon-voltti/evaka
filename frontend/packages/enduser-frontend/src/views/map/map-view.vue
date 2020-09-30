<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <main class="map-view">
    <div class="columns">
      <div class="tab-wrapper column">
        <div class="tab-content">
          <tabs
            isFullwidth
            animation="fade"
            :only-fade="false"
            alignment="centered"
            type="toggle"
            :selectedIndex="selectedTabId"
            @tab-selected="onTabSelected"
          >
            <tab
              class="search-tab"
              :title="$t('map-filters.search')"
              icon="search"
              selected
            >
              <h3 class="title is-3">{{ $t('map-view.search') }}</h3>
              <p>
                {{ $t('map-view.search-text') }}
              </p>
              <p></p>
              <div class="clear-filters has-text-right">
                <a
                  class="clear-btn"
                  href="#"
                  @click.prevent="clearFilters"
                  role="button"
                >
                  <font-awesome-icon
                    :icon="['far', 'trash']"
                  ></font-awesome-icon>
                  {{ $t('map-view.search-clear') }}
                </a>
              </div>
              <address-filter
                @addressFilterChanged="onAddressFilterChanged"
                @addressFilterCleared="onAddressFilterCleared"
                :hideDistance="showDirections"
              >
                <c-tooltip
                  class="is-pulled-right"
                  :message="$t('map-filters.directions')"
                  placement="bottom-end"
                >
                </c-tooltip>
                <span
                  class="button show-directions-btn"
                  @click="displayDirections"
                  :class="{ active: showDirections }"
                  v-if="showDirectionsBtn"
                >
                  <font-awesome-icon
                    :icon="['far', 'level-up']"
                  ></font-awesome-icon>
                </span>
              </address-filter>

              <section v-if="showDirections" class="search-group directions">
                <div class="directions-buttons">
                  <label class="destination-label">
                    {{ $t('map-filters.destination') }}
                  </label>

                  <travel-mode
                    v-if="selectedDestination"
                    v-model="travelMode"
                    :travelModeActive="travelMode"
                  ></travel-mode>
                  <span class="tag distance-tag" v-if="selectedDestination">
                    {{ directionsDistance }} km
                  </span>
                </div>
                <location-picker
                  :addresses="autocompletedDestinations"
                  selectAction="selectDestination"
                  searchAction="searchDestinations"
                  @locationCleared="onDestinationCleared"
                  v-model="selectedDestinationsModel"
                >
                </location-picker>
                <hr />
              </section>

              <language-filter @languageFilterChanged="onLanguageFilterChanged">
              </language-filter>
              <caretype-filter @daycareFilterChanged="onDaycareFilterChanged">
              </caretype-filter>

              <button
                @click="showResults"
                class="button is-primary is-fullwidth show-results"
              >
                {{ $t('map-filters.locations-results') }} ({{ unitsCount }})
                &raquo;
              </button>
            </tab>

            <tab
              class="locations-tab"
              :title="$t('map-filters.locations')"
              icon="list"
            >
              <h3 class="title is-3">{{ $t('map-view.locations') }}</h3>
              <div class="selected-filters">
                <p>{{ $t('map-view.selected-filters') }}:</p>
                <p class="filters-as-text">
                  <span>{{ filterType }}</span>
                  <span v-if="filters.address"
                    >, {{ filters.address.address }}</span
                  >
                </p>
              </div>
              <unit-list class="unit-list" :units="units" @routeTo="onRouteTo">
              </unit-list>
            </tab>
          </tabs>
        </div>
      </div>
      <div class="column map-wrapper">
        <div class="evaka-map">
          <map-component
            :markers="markers"
            :units="filteredUnits"
            :address="address"
            :destination="selectedDestination"
            :travelMode="travelMode"
            @routeTo="onRouteTo"
            @onDirectionsDistance="showDirectionsDistance"
          >
          </map-component>

          <map-overlay></map-overlay>
        </div>
        <footer-component class="map-footer"></footer-component>
      </div>
    </div>
  </main>
</template>

<script>
  import { mapGetters, mapActions } from 'vuex'
  import MapComponent from './map-component'
  import UnitList from './unit-list'
  import AddressFilter from './address-filter'
  import LocationPicker from './location-picker'
  import TravelMode from './travel-mode'
  import TimeFilter from './time-filter'
  import LanguageFilter from './language-filter'
  // CHANGE BACK TO ORIGINAL FOR ALL TYPES
  import CaretypeFilter from './caretype-filter'
  import MapOverlay from './map-overlay'
  import DistrictFilter from './district-filter.vue'
  import Tabs from '@/components/common/tabs-bulma.vue'
  import Tab from '@/components/common/tab-pane-bulma.vue'
  import FooterComponent from '@/components/common/footer-component'
  import { LANGUAGES, UNIT_TYPE } from '@/constants'
  export default {
    data() {
      return {
        selectedTabId: 0,
        showDirections: false,
        travelMode: 'DRIVING',
        selectedDestinationsModel: null,
        directionsDistance: null
      }
    },
    components: {
      FooterComponent,
      MapComponent,
      UnitList,
      AddressFilter,
      TimeFilter,
      CaretypeFilter,
      LanguageFilter,
      DistrictFilter,
      Tabs,
      Tab,
      TravelMode,
      LocationPicker,
      MapOverlay
    },
    computed: {
      ...mapGetters([
        'filters',
        'units',
        'markers',
        'address',
        'selectedDestination',
        'autocompletedDestinations',
        'filteredUnits'
      ]),
      currentLocaleIsSwedish() {
        return this.$i18n.locale.toLowerCase() === LANGUAGES.SV
      },
      units() {
        return _.orderBy(this.filteredUnits, ['name'])
      },
      unitsCount() {
        return this.filteredUnits.length
      },
      showDirectionsBtn() {
        // @TODO: FIX THIS AFTER MVP
        // if (this.address) {
        //   return true
        // }
        return false
      },
      filterType() {
        return Object.values(
          this.$t('map-filters.care-type-options', { returnObjects: true })
        ).find((type) => type.value === this.filters.daycareType).label
      }
    },
    methods: {
      showResults() {
        this.selectedTabId = 1
      },
      onAddressFilterChanged(addressFilter) {
        this.$store.dispatch('applyAddressFilter', addressFilter)
      },
      onAddressFilterCleared() {
        this.$store.dispatch('clearAddressFilter')
        this.showDirections = false
      },
      onDaycareFilterChanged(daycareFilter) {
        this.$store.dispatch('applyDaycareFilter', daycareFilter)
      },
      onLanguageFilterChanged(languageFilter) {
        this.$store.dispatch('applyLanguageFilter', languageFilter)
      },
      onRouteTo(unit) {
        if (!this.showDirections) {
          this.$store.dispatch('disableFilters')
          this.showDirections = true
        }
        this.selectedDestinationsModel = [
          {
            address: unit.name
          }
        ]
        this.$store.dispatch('setDestination', {
          lat: unit.location.lat,
          lng: unit.location.lon
        })
      },
      onTabSelected(selectedTabId) {
        this.selectedTabId = selectedTabId
        if (!this.showDirections) {
          this.$store.dispatch('enableFilters')
        } else {
          this.$store.dispatch('enableDestination')
        }
      },
      onDestinationCleared() {
        this.$store.dispatch('updateUnits')
      },
      displayDirections() {
        this.showDirections = !this.showDirections

        if (this.showDirections) {
          this.$store.dispatch('disableFilters')
        } else {
          this.$store.dispatch('enableFilters')
          this.$store.dispatch('selectDestination', null)
          this.selectedDestinationsModel = null
        }
      },
      showDirectionsDistance(distance) {
        this.directionsDistance = distance
      },
      clearFilters() {
        this.$store.dispatch('initialFilters')
      }
    }
  }
</script>

<style lang="scss" scoped>
  .map-view > .columns {
    flex: 1;
    margin-bottom: 0;
  }

  .tab-wrapper {
    position: relative;
    min-width: 350px;
    max-width: 25%;

    .tab-content {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      overflow-y: scroll;

      // this is a dirty hack
      padding: 1.75rem 1rem 1rem 1.75rem;
    }

    @media (max-width: 767px) {
      .tab-content {
        position: relative;
        padding: 1rem;
      }

      max-width: none;
      min-width: auto;
    }
  }

  .clear-filters {
    margin: 0.75rem 0;

    .clear-btn {
      cursor: pointer;
      color: $primary;
      font-weight: 600;
    }
  }

  .selected-filters {
    margin: 1.5rem 0 0.5rem;

    .filters-as-text {
      font-weight: 600;
    }
  }

  .map-wrapper {
    display: flex;
    flex-direction: column;
    padding-bottom: 0;
  }
</style>
