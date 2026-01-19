import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
    baseURL: '/api',
    timeout: 10000,
})

// 请求拦截器
request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const userStore = useUserStore()
        if (userStore.accessToken) {
            config.headers.Authorization = `Bearer ${userStore.accessToken}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        return response.data
    },
    async (error: AxiosError) => {
        const userStore = useUserStore()

        if (error.response?.status === 401 && userStore.refreshToken) {
            try {
                const res = await axios.post('/api/auth/refresh', null, {
                    headers: { Authorization: `Bearer ${userStore.refreshToken}` }
                })

                if (res.data.code === 200) {
                    userStore.accessToken = res.data.data.accessToken
                    // 重新发起原请求
                    const config = error.config!
                    config.headers.Authorization = `Bearer ${userStore.accessToken}`
                    return request(config)
                }
            } catch {
                userStore.logout()
            }
        }

        return Promise.reject(error)
    }
)

export default request
