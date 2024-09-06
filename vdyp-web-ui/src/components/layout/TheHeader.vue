<template>
  <v-app-bar
    extension-height="10"
    style="height: 64px !important"
    color="#003366"
  >
    <v-img
      alt="B.C. Government Logo"
      :lazy-src="bcLogo"
      :src="bcLogo"
      id="header-bc-logo-img"
    ></v-img>
    <v-spacer></v-spacer>
    <v-toolbar-title
      text="VARIABLE DENSITY YIELD PROJECTION"
      id="header-toolbar-title"
    />
    <v-spacer></v-spacer>

    <v-toolbar-items class="header-right-toolbar">
      <span class="header-training-support">Training and Support</span>
      <span class="header-separator">|</span>

      <v-menu offset-y>
        <template v-slot:activator="{ props }">
          <v-btn v-bind="props" class="d-flex align-center header-user-button">
            <v-icon class="header-user-icon">mdi-account-circle</v-icon>
            <span class="header-user-name"
              >{{ userInfo?.given_name }} {{ userInfo?.family_name }}</span
            >
          </v-btn>
        </template>

        <v-list>
          <v-list-item @click="logout">
            <v-list-item-title>Logout</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-toolbar-items>
  </v-app-bar>
</template>
<script setup lang="ts">
import bcLogo from '@/assets/gov-bc-logo-horiz.png'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/common/authStore'

const authStore = useAuthStore()
const userInfo = computed(() => authStore.parseIdToken())

const logout = () => {
  authStore.logout()
}
</script>
<style scoped>
/* v-app-bar whole box */
header.v-toolbar.v-toolbar--density-default.v-theme--defaultTheme.v-locale--is-ltr.v-app-bar {
  justify-content: center;
}

#header-bc-logo-img {
  max-height: 50px;
  max-width: 150px;
  margin-left: 15px;
}

#header-toolbar-title {
  text-align: center;
  flex: 1;
  font-weight: 300;
}

/* right-toolbar whole box */
div.v-toolbar-items.header-right-toolbar {
  padding-right: 10px;
}

.header-right-toolbar {
  padding-right: 16px;
  display: flex;
  align-items: center;
}

.header-right-toolbar span {
  padding: 0 8px;
}

span.header-training-support {
  font-size: 14px;
}

span.header-separator {
  padding: 0px;
}

/* user-icon and user-name whole box */
button.v-btn.v-theme--defaultTheme.v-btn--density-default.v-btn--size-default.v-btn--variant-text.d-flex.align-center {
  padding-left: 5px;
  padding-right: 5px;
}

/* user-icon*/
i.mdi-account-circle.mdi.v-icon.notranslate.v-theme--defaultTheme.v-icon--size-default.header-user-icon {
  margin-top: 3px;
}

span.header-user-icon {
  margin-right: 4px;
  padding-top: 1px;
}

span.header-user-name {
  padding: 0px 0px 0px 3px;
  text-transform: none;
  font-size: 14px;
  letter-spacing: normal;
  line-height: 21px;
}

.header-user-button {
  padding-left: 0px;
  padding-right: 0px;
  min-width: auto;
}
</style>
