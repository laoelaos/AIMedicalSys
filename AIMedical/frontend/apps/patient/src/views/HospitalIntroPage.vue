<template>
  <div class="hospital-container">
    <div class="page-header">
      <el-button type="default" @click="router.push('/home')">← 返回首页</el-button>
      <h2>医院介绍</h2>
    </div>

    <el-card shadow="hover" class="intro-card">
      <h3>🏥 智慧云脑附属医院</h3>
      <div class="intro-body">
        <p><strong>医院等级：</strong>三级甲等综合医院</p>
        <p><strong>建院时间：</strong>2010 年</p>
        <p><strong>医院地址：</strong>北京市海淀区中关村大街 100 号</p>
        <p><strong>联系电话：</strong>010-8888-0001</p>
        <p><strong>交通路线：</strong>地铁 4 号线中关村站 A 口出，步行约 300 米</p>
      </div>
    </el-card>

    <el-card shadow="hover" class="intro-card">
      <h3>科室设置</h3>
      <div class="department-grid">
        <el-tag v-for="dept in departments" :key="dept" type="info" size="large">{{ dept }}</el-tag>
      </div>
    </el-card>

    <el-card shadow="hover" class="intro-card">
      <h3>就诊指南</h3>
      <el-timeline>
        <el-timeline-item timestamp="08:00-17:30" placement="top">门诊时间</el-timeline-item>
        <el-timeline-item timestamp="一层大厅" placement="top">挂号/缴费/取药</el-timeline-item>
        <el-timeline-item timestamp="二层-五层" placement="top">各科室诊室</el-timeline-item>
        <el-timeline-item timestamp="六层" placement="top">住院部/手术室</el-timeline-item>
        <el-timeline-item timestamp="地下一层" placement="top">影像检查 (CT/MRI/X光)</el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const departments: string[] = [
  '内科', '外科', '妇产科', '儿科', '骨科', '眼科', '耳鼻喉科',
  '皮肤科', '神经内科', '心内科', '消化内科', '呼吸内科',
  '泌尿外科', '肿瘤科', '康复科', '口腔科'
]
</script>

<style scoped>
.hospital-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.intro-card {
  margin-bottom: 16px;
}

.intro-card h3 {
  margin-top: 0;
}

.intro-body p {
  margin: 6px 0;
  font-size: 14px;
  color: #606266;
}

.department-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.department-grid .el-tag {
  font-size: 13px;
  padding: 6px 14px;
}
</style>
