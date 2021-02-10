<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <multi-select
    class="location-picker"
    :options="addresses"
    :placeholder="$t('map-filters.address-placeholder')"
    selectLabel=""
    label="address"
    selectedLabel=""
    deselectLabel=""
    :max-height="500"
    :showNoOptions="false"
    :clear-on-select="true"
    multiple
    :max="!hasValue ? -1 : 1"
    :searchable="hasValue"
    @select="updateSelected"
    @remove="removeSelected"
    @search-change="onSearchChanged"
    :value="value"
  >
    <span slot="noResult">
      {{ $t('map-filters.address-not-found') }}
    </span>
  </multi-select>
</template>

<script>
  import MultiSelect from 'vue-multiselect'
  import _ from 'lodash'
  import { config } from '@/config'

  export default {
    props: {
      addresses: Array,
      selectAction: String,
      searchAction: String,
      value: {}
    },
    components: {
      MultiSelect
    },
    methods: {
      updateSelected(selectedValues) {
        this.$emit('input', selectedValues)
        this.$store.dispatch(this.selectAction, selectedValues)
      },
      removeSelected() {
        this.$emit('input', null)
        this.$emit('locationCleared')
        this.$store.dispatch(this.selectAction, null)
      },
      onSearchChanged: _.debounce(function(query) {
        this.$store.dispatch(this.searchAction, query)
      }, config.filters.autocompleteDebounceWaitMs)
    },
    computed: {
      hasValue() {
        return this.value === null || this.value.length === 0
      }
    }
  }
</script>

<!-- not using scoped because multiselect style override wouldn't work -->
<style lang="scss">
  .location-picker {
    .multiselect__single {
      font-style: italic;
      font-size: 0.875rem;
      opacity: 0.75;
    }
    .multiselect__select {
      display: none;
    }

    .multiselect__tags {
      position: relative;
      padding: 8px 8px 0 8px;
    }

    .multiselect__tag {
      background: #3273dc;
      max-width: 85%;
      position: relative;
      padding: 4px 26px 3px 10px;
      margin-bottom: 0;

      > span {
        display: inline-block;
        max-width: 100%;
        overflow: hidden;
      }

      &-icon {
        line-height: 21px;
        &:hover {
          background: #7aa3e0;

          &:after {
            color: white;
          }
        }

        &:after {
          color: #032b79;
        }
      }
    }

    .multiselect__content {
      z-index: 999;
    }
  }
</style>
