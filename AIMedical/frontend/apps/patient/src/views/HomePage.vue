<template>
  <div class="home-container">
    <div class="home-header">
      <h2>智慧云脑诊疗平台</h2>
      <p class="greeting">您好，{{ profile?.name || '用户' }}</p>
      <el-button type="default" size="small" @click="handleLogout">退出登录</el-button>
    </div>

    <div class="service-grid">
      <div class="service-item" v-for="item in serviceEntries" :key="item.key" @click="handleService(item)">
        <div class="service-icon" :style="{ background: item.color }">
          <span class="icon-text">{{ item.icon }}</span>
        </div>
        <span class="service-label">{{ item.label }}</span>
      </div>
    </div>

    <div v-if="showConvenience" class="convenience-section">
      <el-divider />
      <h3>便民服务区</h3>
      <div class="convenience-grid">
        <el-card shadow="hover" class="convenience-card" @click="router.push('/fee-query')">
          <div class="card-row">
            <span class="card-icon">💰</span>
            <span>费用查询</span>
          </div>
          <p class="card-desc">查询门诊、住院、检查等各项费用明细</p>
        </el-card>

        <el-card shadow="hover" class="convenience-card" @click="router.push('/hospital-intro')">
          <div class="card-row">
            <span class="card-icon">🏥</span>
            <span>医院介绍</span>
          </div>
          <p class="card-desc">了解医院概况、科室设置、专家团队</p>
        </el-card>

        <el-card shadow="hover" class="convenience-card" @click="handlePlaceholder('院内导航')">
          <div class="card-row">
            <span class="card-icon">📍</span>
            <span>院内导航</span>
          </div>
          <p class="card-desc">查看各科室楼层分布，方便快速就诊</p>
        </el-card>
      </div>
    </div>

    <!-- AI 智能导诊浮标 -->
    <div class="fab-triage" @click="router.push('/triage')">
      <span class="fab-icon">🤖</span>
      <span class="fab-label">AI导诊</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const profile = ref(auth.profile)
const showConvenience = ref(false)

interface ServiceEntry {
  key: string
  label: string
  icon: string
  color: string
  route?: string
}

const serviceEntries: ServiceEntry[] = [
  { key: 'registration', label: '门诊挂号', icon: '🏥', color: '#409EFF', route: '/registration' },
  { key: 'online-consult', label: '在线问诊', icon: '💬', color: '#67C23A', route: '/consult' },
  { key: 'exam-booking', label: '预约检查', icon: '📋', color: '#E6A23C', route: undefined },
  { key: 'report-query', label: '报告查询', icon: '📊', color: '#F56C6C', route: '/records' },
  { key: 'pharmacy', label: '药房取药', icon: '💊', color: '#909399', route: undefined },
  { key: 'inpatient', label: '住院服务', icon: '🛏️', color: '#8B5CF6', route: undefined },
  { key: 'health', label: '健康档案', icon: '❤️', color: '#EC4899', route: '/profile' },
  { key: 'convenience', label: '便民服务区', icon: '🔧', color: '#14B8A6', route: undefined },
  { key: 'fee-history', label: '缴费记录', icon: '📄', color: '#F97316', route: '/records' },
]

function handleService(item: ServiceEntry) {
  if (item.key === 'inpatient') {
    ElMessageBox.alert('该功能正在开发中，敬请期待上线', '住院服务暂未上线', {
      confirmButtonText: '知道了',
      type: 'info',
    })
  } else if (item.key === 'convenience') {
    showConvenience.value = !showConvenience.value
  } else if (item.route) {
    router.push(item.route)
  } else {
    handlePlaceholder(item.label)
  }
}

function handlePlaceholder(name: string) {
  ElMessage.info(`${name}功能建设中，敬请期待`)
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  if (auth.profile) {
    profile.value = auth.profile
  }
})
</script>

<style scoped>
.home-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.home-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.home-header h2 {
  margin: 0;
  font-size: 22px;
  flex: 1;
}

.greeting {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.service-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.service-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.service-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-text {
  font-size: 26px;
}

.service-label {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.convenience-section {
  margin-top: 8px;
}

.convenience-section h3 {
  margin: 0 0 14px 0;
  font-size: 17px;
}

.convenience-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.convenience-card {
  cursor: pointer;
  transition: box-shadow 0.15s;
}

.convenience-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12) !important;
}

.card-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
}

.card-icon {
  font-size: 20px;
}

.card-desc {
  margin: 6px 0 0 28px;
  font-size: 13px;
  color: #909399;
}

.fab-triage {
  position: fixed;
  right: 24px;
  bottom: 80px;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #67C23A);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  z-index: 100;
}

.fab-triage:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(64, 158, 255, 0.55);
}

.fab-icon {
  font-size: 26px;
  line-height: 1;
}

.fab-label {
  font-size: 10px;
  color: #fff;
  margin-top: 2px;
}
</style>
