<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="travel-mode-container">
    <label
      class="travel-mode"
      title="Autolla"
      :class="{ active: this.travelModeActive == 'DRIVING' }"
    >
      <input type="radio" id="driving" value="DRIVING" v-model="travelMode" />
      <i class="material-icons">directions_car</i>
    </label>
    <label
      class="travel-mode"
      title="Kävellen"
      :class="{ active: this.travelModeActive == 'WALKING' }"
    >
      <input type="radio" id="walking" value="WALKING" v-model="travelMode" />
      <i class="material-icons">directions_walk</i>
    </label>
    <label
      class="travel-mode"
      title="Pyöräillen"
      :class="{ active: this.travelModeActive == 'BICYCLING' }"
    >
      <input
        type="radio"
        id="bicycling"
        value="BICYCLING"
        v-model="travelMode"
      />
      <i class="material-icons">directions_bike</i>
    </label>
    <label
      class="travel-mode"
      title="Julkisilla kulkuneuvoilla"
      :class="{ active: this.travelModeActive == 'TRANSIT' }"
    >
      <input type="radio" id="transit" value="TRANSIT" v-model="travelMode" />
      <i class="material-icons">directions_transit</i>
    </label>
  </div>
</template>

<script>
  export default {
    data() {
      return {
        travelMode: 'DRIVING'
      }
    },
    props: {
      travelModeActive: {
        type: String
      }
    },
    watch: {
      travelMode(mode) {
        this.$emit('input', mode)
      }
    }
  }
</script>

<style lang="scss">
  .travel-mode-container {
    display: inline-block;
    text-align: right;

    &:after {
      content: '';
      display: table;
      clear: both;
    }
  }

  .travel-mode {
    display: inline-block;
    position: relative;
    width: 2rem;
    height: 2rem;
    padding-top: 3px;
    margin-left: 0.25rem;
    cursor: pointer;
    border: 1px solid;
    border-radius: 6px;
    text-align: center;

    &.active {
      color: white;
      background-color: #3273dc;
      border-color: #3273dc;
    }

    input {
      display: none;
    }

    &:hover {
      color: white;
      background-color: #3273dc;
    }
  }
</style>
