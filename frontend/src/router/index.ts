import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/LoginView.vue'),
            meta: { requiresAuth: false }
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/RegisterView.vue'),
            meta: { requiresAuth: false }
        },
        {
            path: '/',
            component: () => import('@/layouts/MainLayout.vue'),
            children: [
                {
                    path: '',
                    name: 'Home',
                    component: () => import('@/views/HomeView.vue'),
                    meta: { requiresAuth: true }
                },
                {
                    path: 'learn',
                    name: 'Learn',
                    component: () => import('@/views/LearnView.vue'),
                    meta: { requiresAuth: true }
                },
                {
                    path: 'vocabulary',
                    name: 'Vocabulary',
                    component: () => import('@/views/VocabularyView.vue'),
                    meta: { requiresAuth: true }
                },
                {
                    path: 'review',
                    name: 'Review',
                    component: () => import('@/views/ReviewView.vue'),
                    meta: { requiresAuth: true }
                }
            ]
        },
    ]
})

router.beforeEach((to, _from, next) => {
    const userStore = useUserStore()

    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
        next({ name: 'Login' })
    } else if (!to.meta.requiresAuth && userStore.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
        next({ name: 'Home' })
    } else {
        next()
    }
})

export default router
