<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <nav>
    <div class="menu-offset">
      <router-link to="/" exact>
        <img class="logo" src="../assets/espoo-logo.svg" alt="Espoo logo" />
      </router-link>
    </div>
    <div class="nav-menu">
      <router-link
        :key="index"
        v-for="(item, index) in model"
        exact
        active-class="is-active"
        class="menu-item"
        :to="{ path: item.path }"
      >
        <font-awesome-icon :icon="item.icon"></font-awesome-icon>
        {{ item.label | uppercase }}
      </router-link>
      <a
        v-if="showCitizenFrontendLink"
        class="menu-item"
        :href="newDecisionsUrl"
        data-qa="nav-decisions"
      >
        <font-awesome-icon :icon="['fal', 'gavel']"></font-awesome-icon>
        {{ $t('menu.routes.newDecisions') | uppercase }}
      </a>

      <div class="spacer" />
      <div class="nav-menuRight">
        <div
          class="navbar-item navbar-language"
          tabindex="0"
          @keyup.enter="isChangeLanguageClicked = !isChangeLanguageClicked"
        >
          <div
            class="dropdown-container"
            @click="isChangeLanguageClicked = !isChangeLanguageClicked"
          >
            <div class="dropdown-menu-wrapper">
              <span>{{
                $t(`menu.language.${selectedLanguage}.value`).toUpperCase()
              }}</span>
              <font-awesome-icon
                :icon="[
                  'fal',
                  isChangeLanguageClicked ? 'angle-up' : 'angle-down'
                ]"
              ></font-awesome-icon>
            </div>
            <div
              class="dropdown-content-wrapper"
              v-if="isChangeLanguageClicked"
            >
              <div class="languages">
                <div
                  :class="[
                    'language-item',
                    { 'is-selected': selectedLanguage === language.value }
                  ]"
                  @click="setlanguage(language.value)"
                  @keyup.enter="setlanguage(language.value)"
                  v-for="(language, index) in languages"
                  :key="index"
                  tabindex="0"
                >
                  <div>
                    {{
                      $t(`menu.language.${language.value}.value`).toUpperCase()
                    }}
                  </div>
                  <div>{{ $t(`menu.language.${language.value}.label`) }}</div>
                  <div>
                    <font-awesome-icon
                      :icon="['fal', 'check']"
                      v-if="selectedLanguage === language.value"
                    ></font-awesome-icon>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <a
          v-if="isLoggedIn"
          id="logout-btn"
          type="button"
          class="menu-item login"
          data-qa="logout-btn"
          :href="logoutAddress"
        >
          <div class="icon round">
            <font-awesome-icon :icon="['fal', 'sign-out']"></font-awesome-icon>
          </div>
          <div class="user-info">
            <div>{{ userName }}</div>
            <div>{{ $t('menu.logout') | uppercase }}</div>
          </div>
        </a>

        <a id="login-btn" v-else class="menu-item login" :href="loginAddress" data-qa="login-btn">
          <div class="icon round">
            <font-awesome-icon :icon="['fal', 'sign-in']"></font-awesome-icon>
          </div>
          {{ $t('menu.login') | uppercase }}
        </a>
      </div>
    </div>
  </nav>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { config } from '@evaka/enduser-frontend/src/config'

  export default {
    data() {
      return {
        isChangeLanguageClicked: false
      }
    },
    props: {
      loginAddress: {
        type: String,
        required: true
      },
      logoutAddress: {
        type: String,
        required: true
      },
      model: {
        type: Array,
        required: true
      },
      citizenFrontendUrl: {
        type: String,
        required: true
      }
    },
    computed: {
      ...mapGetters(['isLoggedIn', 'userName']),
      selectedLanguage() {
        return this.$i18n.locale
      },
      languages() {
        return Object.values(this.$t('menu.language', { returnObjects: true }))
      },
      showCitizenFrontendLink() {
        return this.isLoggedIn && config.feature.citizenFrontend
      },
      newDecisionsUrl() {
        return `${this.citizenFrontendUrl}/decisions`
      }
    },
    methods: {
      setlanguage(language) {
        this.$i18n.locale = language
        localStorage.setItem('enduser_language', language)
      }
    }
  }
</script>

<style lang="scss" scoped>
  nav {
    position: relative;
    display: flex;
    flex-direction: row;
    height: 4rem;
    background-color: $blue;
    .logo {
      padding: 0 1.5rem;
      margin-left: 1rem;
      width: auto;
      min-width: 150px;
      height: 100%;
    }
    @include onMobile() {
      display: none;
    }
  }
  .menu-offset {
    display: flex;
    flex-direction: row;
    flex: 0 1 25%;
  }
  .nav-menu {
    display: flex;
    @include onMobile() {
      display: none;
    }
    flex: 1 0 auto;
    flex-direction: row;
  }
  .nav-menuRight {
    display: flex;
    justify-self: flex-end;
    .user-info {
      text-align: left;
    }
    .icon {
      width: 2.5rem;
      height: 2.5rem;
      margin-right: 10px;
      &.round {
        background-color: $soft-blue;
        border-radius: 100%;
        padding-left: 0.5rem;
      }
    }

    .navbar-language {
      margin-bottom: 0.25rem;
    }

    .dropdown-container {
      align-items: center;
      display: flex;
      height: 100%;
    }

    .dropdown-menu-wrapper {
      color: white;
      cursor: pointer;

      & :nth-child(2) {
        margin-left: 0.5rem;
      }
    }

    .dropdown-content-wrapper {
      position: absolute;
      top: 4rem;

      .languages {
        background: $white;
        box-shadow: 0 2px 6px 0 $grey-lighter;
        border-radius: 0;
        cursor: pointer;
        width: 10.5rem;

        .language-item {
          display: flex;
          padding: 10px;

          &.is-selected {
            font-weight: 600;
          }

          &:hover {
            background-color: tint($blue, 60%);
            z-index: 1;
          }

          & :nth-child(1) {
            width: 1.8rem;
          }
        }
      }
    }
  }

  .spacer {
    flex: 1 1 auto;
  }
  form {
    display: flex;
    flex-direction: row;
  }
  .menu-item {
    border: none;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
    color: white;
    min-width: 140px;
    padding: 0 16px;
    border-bottom: 4px solid transparent;
    background-color: transparent;
    font-size: 14px;
    &:hover {
      border-bottom: 4px solid fade_out($soft-blue, 0.5);
    }
    &.is-active {
      border-bottom: 4px solid $soft-blue;
    }
    svg {
      margin-right: 0.5em;
      font-size: 1rem;
    }
  }
</style>
