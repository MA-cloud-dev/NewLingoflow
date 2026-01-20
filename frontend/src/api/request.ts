import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
    baseURL: '/api',
    timeout: 10000,
})

// 标记是否正在刷新 token，避免多个请求同时触发刷新
let isRefreshing = false
// 存储等待刷新完成的请求队列
let refreshSubscribers: ((token: string) => void)[] = []

// 通知所有等待的请求
function onRefreshed(token: string) {
    refreshSubscribers.forEach(callback => callback(token))
    refreshSubscribers = []
}

// 添加请求到等待队列
function addRefreshSubscriber(callback: (token: string) => void) {
    refreshSubscribers.push(callback)
}

// 处理 token 过期，清除数据并跳转登录页
function handleTokenExpired() {
    const userStore = useUserStore()
    userStore.logout()
    // 跳转到登录页面
    router.push({ name: 'Login' })
}

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
        const originalRequest = error.config!

        // 处理 401 错误
        if (error.response?.status === 401) {
            // 如果有 refreshToken，尝试刷新
            if (userStore.refreshToken) {
                // 如果正在刷新，将请求加入队列等待
                if (isRefreshing) {
                    return new Promise(resolve => {
                        addRefreshSubscriber((token: string) => {
                            originalRequest.headers.Authorization = `Bearer ${token}`
                            resolve(request(originalRequest))
                        })
                    })
                }

                isRefreshing = true

                try {
                    const res = await axios.post('/api/auth/refresh', null, {
                        headers: { Authorization: `Bearer ${userStore.refreshToken}` }
                    })

                    if (res.data.code === 200) {
                        const newAccessToken = res.data.data.accessToken
                        userStore.accessToken = newAccessToken

                        // 通知所有等待的请求
                        onRefreshed(newAccessToken)

                        // 重新发起原请求
                        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
                        return request(originalRequest)
                    } else {
                        // 刷新失败，跳转登录页
                        handleTokenExpired()
                    }
                } catch {
                    // 刷新失败，跳转登录页
                    handleTokenExpired()
                } finally {
                    isRefreshing = false
                }
            } else {
                // 没有 refreshToken，直接跳转登录页
                handleTokenExpired()
            }
        }

        return Promise.reject(error)
    }
)

export default request
