import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../views/LoginPage.vue'

function mountLoginPage() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', component: LoginPage },
      { path: '/register', component: { template: '<div>Register</div>' } },
      { path: '/profile', component: { template: '<div>Profile</div>' } },
    ],
  })

  return mount(LoginPage, {
    global: {
      plugins: [router],
      stubs: {
        'el-card': { template: '<div class="el-card"><slot/><slot name="header"/></div>' },
        'el-form': { template: '<div><slot/></div>' },
        'el-form-item': { template: '<div><slot/></div>' },
        'el-input': { template: '<input/>' },
        'el-button': { template: '<button><slot/></button>' },
      },
    },
  })
}

describe('LoginPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders login page title', () => {
    const wrapper = mountLoginPage()
    expect(wrapper.html()).toContain('患者登录')
  })

  it('renders register link', () => {
    const wrapper = mountLoginPage()
    expect(wrapper.html()).toContain('还没有账号？立即注册')
  })

  it('is a valid Vue component', () => {
    const wrapper = mountLoginPage()
    expect(wrapper.vm).toBeTruthy()
  })
})
