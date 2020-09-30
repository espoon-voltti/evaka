<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="search-group">
    <span>Alue:</span>
    <select-list
      v-model="selectedDistrict"
      name="selectedDistrict"
      class="select-district"
      :options="districtNames"
      placeholder="Valitse alue"
      @input="onInput"
    ></select-list>
  </div>
</template>

<script>
  export default {
    data() {
      return {
        selectedDistrict: ''
      }
    },
    props: ['espooDistricts'],
    methods: {
      onInput() {
        const district = this.espooDistricts.features.find(
          (a) => a.properties.ID1 === this.selectedDistrict
        )
        this.$emit(
          'districtFilterChanged',
          district !== null ? district : this.selectedDistrict
        )
      }
    },
    computed: {
      districtNames() {
        const districts = this.espooDistricts.features
        return districts.map(function(a) {
          return {
            value: a.properties.name,
            name: a.properties.ID1
          }
        })
      }
    }
  }
</script>
