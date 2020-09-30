<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="version">
    <h3>Osajärjestelmä {{ info.name }}</h3>
    <template v-for="(value, key) in info">
      <span class="key">{{ key }}: </span>
      <span class="value" :class="{ error: key == 'error' }">{{ value }}</span>
      <br />
    </template>
  </div>
</template>

<script>
  export default {
    props: ['info']
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped="true">
  h3 {
    font-size: larger;
  }
  .version {
    padding-left: 20px;
    padding-top: 20px;
    text-align: left;
  }
  .key {
    font-weight: bold;
  }
  .error {
    color: red;
  }
</style>
