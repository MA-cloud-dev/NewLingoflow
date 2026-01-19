import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
    const accessToken = ref<string | null>(null)
    const refreshToken = ref<string | null>(null)
    const userInfo = ref<LoginResponse['user'] | null>(null)

    const isLoggedIn = computed(() => !!accessToken.value)

    function setLoginData(data: LoginResponse) {
        accessToken.value = data.accessToken
        refreshToken.value = data.refreshToken
        userInfo.value = data.user
    }

    function logout() {
        accessToken.value = null
        refreshToken.value = null
        userInfo.value = null
    }

    return {
        accessToken,
        refreshToken,
        userInfo,
        isLoggedIn,
        setLoginData,
        logout,
    }
}, {
    persist: true,
})
