<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <ol>
    <li v-for="unit in units" :key="unit.id">
      <unit-details :unit="unit" @routeTo="onRouteTo(unit)"> </unit-details>
    </li>
  </ol>
</template>

<script>
  import UnitDetails from '@/views/map/unit-details.vue'

  export default {
    props: {
      units: Array
    },
    components: {
      UnitDetails
    },
    methods: {
      onRouteTo(unit) {
        this.$emit('routeTo', unit)
      }
    }
  }
</script>

<style lang="scss" scoped>
  ol {
    padding: 0;
    text-align: left;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    list-style-type: none;
  }

  li {
    list-style-position: inside;
    width: 100%;
    padding: 0.825rem 0;
    margin: 0 0 1rem 0;
    border-bottom: 1px solid #ccc;
  }
</style>
