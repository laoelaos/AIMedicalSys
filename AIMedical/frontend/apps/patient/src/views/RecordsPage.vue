<template>
  <div class="records-container">
    <div class="page-header">
      <el-button type="default" @click="router.push('/home')">← 返回首页</el-button>
      <h2>报告 / 病历 / 处方 / 缴费</h2>
    </div>

    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <!-- 报告查询 -->
      <el-tab-pane label="报告查询" name="reports" lazy>
        <div class="tab-content">
          <div v-if="reports.loading" class="loading-box">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="reports.error" class="error-box">
            <el-result icon="error" title="加载失败" :sub-title="reports.error">
              <template #extra>
                <el-button type="primary" @click="loadReports">重试</el-button>
              </template>
            </el-result>
          </div>

          <div v-else-if="reports.list.length === 0" class="empty-box">
            <el-empty description="暂无检查/检验报告" />
          </div>

          <div v-else-if="!reports.detail" class="list-view">
            <div
              v-for="r in reports.list"
              :key="r.id"
              class="report-item"
              @click="openReportDetail(r)"
            >
              <div class="item-main">
                <div class="item-name">
                  <el-tag :type="r.type === '检查' ? 'primary' : 'success'" size="small">{{ r.type }}</el-tag>
                  <span class="item-title">{{ r.name }}</span>
                </div>
                <span class="item-status">{{ r.status }}</span>
              </div>
              <div class="item-sub">
                <span>{{ r.exam_date }}</span>
                <span v-if="r.department_name">{{ r.department_name }}</span>
              </div>
            </div>
          </div>

          <div v-else class="detail-view">
            <el-button text type="primary" @click="reports.detail = null">← 返回列表</el-button>
            <el-card class="detail-card">
              <template #header>
                <span>{{ reports.detail.name }}</span>
              </template>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="检查日期">{{ reports.detail.exam_date }}</el-descriptions-item>
                <el-descriptions-item label="报告日期">{{ reports.detail.report_date }}</el-descriptions-item>
                <el-descriptions-item label="科室">{{ reports.detail.department_name || '-' }}</el-descriptions-item>
                <el-descriptions-item label="医生">{{ reports.detail.doctor_name || '-' }}</el-descriptions-item>
              </el-descriptions>
              <div class="detail-summary">{{ reports.detail.summary }}</div>
              <el-table v-if="reports.detail.details?.length" :data="reports.detail.details" size="small" class="detail-table">
                <el-table-column prop="item" label="项目" />
                <el-table-column prop="result" label="结果" />
                <el-table-column prop="reference_range" label="参考范围" />
                <el-table-column prop="unit" label="单位" width="70" />
                <el-table-column prop="flag" label="标志" width="60" />
              </el-table>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <!-- 病历查询 -->
      <el-tab-pane label="病历查询" name="medical" lazy>
        <div class="tab-content">
          <div v-if="medical.loading" class="loading-box">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="medical.error" class="error-box">
            <el-result icon="error" title="加载失败" :sub-title="medical.error">
              <template #extra>
                <el-button type="primary" @click="loadMedical">重试</el-button>
              </template>
            </el-result>
          </div>

          <div v-else-if="medical.list.length === 0" class="empty-box">
            <el-empty description="暂无电子病历记录" />
          </div>

          <div v-else-if="!medical.detail" class="list-view">
            <div
              v-for="r in medical.list"
              :key="r.id"
              class="report-item"
              @click="medical.detail = r"
            >
              <div class="item-main">
                <span class="item-title">{{ r.department_name }} · {{ r.visit_date }}</span>
              </div>
              <div class="item-sub">
                <span>{{ r.doctor_name }}</span>
                <span>{{ r.diagnosis }}</span>
              </div>
            </div>
          </div>

          <div v-else class="detail-view">
            <el-button text type="primary" @click="medical.detail = null">← 返回列表</el-button>
            <el-card class="detail-card">
              <template #header><span>病历详情</span></template>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="就诊日期">{{ medical.detail.visit_date }}</el-descriptions-item>
                <el-descriptions-item label="就诊科室">{{ medical.detail.department_name }}</el-descriptions-item>
                <el-descriptions-item label="主诊医生">{{ medical.detail.doctor_name }}</el-descriptions-item>
                <el-descriptions-item label="初步诊断">{{ medical.detail.diagnosis }}</el-descriptions-item>
              </el-descriptions>
              <div class="detail-section">
                <h4>主诉</h4>
                <p>{{ medical.detail.chief_complaint }}</p>
              </div>
              <div class="detail-section">
                <h4>医嘱</h4>
                <p>{{ medical.detail.advice }}</p>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <!-- 处方查询 -->
      <el-tab-pane label="处方查询" name="prescriptions" lazy>
        <div class="tab-content">
          <div v-if="prescriptions.loading" class="loading-box">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="prescriptions.error" class="error-box">
            <el-result icon="error" title="加载失败" :sub-title="prescriptions.error">
              <template #extra>
                <el-button type="primary" @click="loadPrescriptions">重试</el-button>
              </template>
            </el-result>
          </div>

          <div v-else-if="prescriptions.list.length === 0" class="empty-box">
            <el-empty description="暂无电子处方记录" />
          </div>

          <div v-else-if="!prescriptions.detail" class="list-view">
            <div
              v-for="r in prescriptions.list"
              :key="r.id"
              class="report-item"
              @click="prescriptions.detail = r"
            >
              <div class="item-main">
                <span class="item-title">{{ r.department_name }}</span>
                <el-tag
                  :type="r.status === '已发药' ? 'success' : r.status === '已审核' ? 'primary' : 'warning'"
                  size="small"
                >{{ r.status }}</el-tag>
              </div>
              <div class="item-sub">
                <span>{{ r.doctor_name }}</span>
                <span>{{ r.issue_date }}</span>
              </div>
            </div>
          </div>

          <div v-else class="detail-view">
            <el-button text type="primary" @click="prescriptions.detail = null">← 返回列表</el-button>
            <el-card class="detail-card">
              <template #header><span>处方详情</span></template>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="开方日期">{{ prescriptions.detail.issue_date }}</el-descriptions-item>
                <el-descriptions-item label="开方医生">{{ prescriptions.detail.doctor_name }}</el-descriptions-item>
                <el-descriptions-item label="科室">{{ prescriptions.detail.department_name }}</el-descriptions-item>
                <el-descriptions-item label="处方状态">{{ prescriptions.detail.status }}</el-descriptions-item>
              </el-descriptions>
              <div class="detail-section">
                <h4>诊断</h4>
                <p>{{ prescriptions.detail.diagnosis }}</p>
              </div>

              <el-table :data="prescriptions.detail.medications" size="small" class="detail-table" show-summary>
                <el-table-column prop="drug_name" label="药品名称" />
                <el-table-column prop="dosage" label="规格" />
                <el-table-column prop="frequency" label="用法" />
                <el-table-column prop="duration" label="疗程" />
                <el-table-column prop="quantity" label="数量" width="60" />
                <el-table-column prop="price" label="单价" width="70" />
              </el-table>

              <div class="ai-review">
                <div class="ai-review-header">
                  <span class="ai-icon">🤖</span>
                  <span>AI 处方审核</span>
                </div>
                <p class="ai-review-text">
                  {{ prescriptions.detail.ai_review_summary || 'AI 处方审核报告生成中，请稍后查看…' }}
                </p>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <!-- 缴费记录 -->
      <el-tab-pane label="缴费记录" name="payments" lazy>
        <div class="tab-content">
          <div class="filter-bar">
            <el-date-picker
              v-model="paymentFilters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="small"
              style="width:260px"
              @change="loadPayments"
            />
            <el-select
              v-model="paymentFilters.category"
              placeholder="费用类别"
              clearable
              size="small"
              style="width:130px"
              @change="loadPayments"
            >
              <el-option label="全部" value="" />
              <el-option label="挂号" value="挂号" />
              <el-option label="检查" value="检查" />
              <el-option label="检验" value="检验" />
              <el-option label="药费" value="药费" />
              <el-option label="其他" value="其他" />
            </el-select>
            <span class="filter-total">合计：<strong>¥{{ totalAmount }}</strong></span>
          </div>

          <div v-if="payments.loading" class="loading-box">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="payments.error" class="error-box">
            <el-result icon="error" title="加载失败" :sub-title="payments.error">
              <template #extra>
                <el-button type="primary" @click="loadPayments">重试</el-button>
              </template>
            </el-result>
          </div>

          <div v-else-if="payments.list.length === 0" class="empty-box">
            <el-empty description="暂无缴费记录" />
          </div>

          <div v-else class="list-view">
            <div v-for="p in payments.list" :key="p.id" class="payment-item">
              <div class="item-main">
                <div class="payment-left">
                  <el-tag :type="p.category === '药费' ? '' : p.category === '检查' ? 'primary' : 'success'" size="small">{{ p.category }}</el-tag>
                  <span class="item-title">{{ p.project_name }}</span>
                </div>
                <div class="payment-right">
                  <span class="payment-amount">¥{{ p.amount }}</span>
                  <el-tag
                    :type="p.status === '已缴费' ? 'success' : p.status === '待缴费' ? 'warning' : 'info'"
                    size="small"
                  >{{ p.status }}</el-tag>
                </div>
              </div>
              <div class="item-sub">
                <span v-if="p.visit_date">就诊日期：{{ p.visit_date }}</span>
                <span v-if="p.payment_date">缴费日期：{{ p.payment_date }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 分诊记录 -->
      <el-tab-pane label="分诊记录" name="triage" lazy>
        <div class="tab-content">
          <div class="filter-bar">
            <el-checkbox v-model="triageFilters.degradedOnly" @change="loadTriage">仅显示降级记录</el-checkbox>
          </div>

          <div v-if="triage.loading" class="loading-box">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="triage.error" class="error-box">
            <el-result icon="error" title="加载失败" :sub-title="triage.error">
              <template #extra>
                <el-button type="primary" @click="loadTriage">重试</el-button>
              </template>
            </el-result>
          </div>

          <div v-else-if="triage.list.length === 0" class="empty-box">
            <el-empty description="暂未使用过 AI 导诊" />
          </div>

          <div v-else class="list-view">
            <div v-for="r in triage.list" :key="r.id" class="triage-item">
              <div class="item-main">
                <span class="item-title">主诉：{{ r.chief_complaint }}</span>
                <el-tag :type="r.is_degraded ? 'warning' : 'success'" size="small">
                  {{ r.is_degraded ? '已降级' : '正常' }}
                </el-tag>
              </div>
              <div class="item-sub">
                <span>推荐科室：{{ r.recommended_departments || '-' }}</span>
              </div>
              <div class="item-sub">
                <span>推荐医生：{{ r.recommended_doctors || '-' }}</span>
              </div>
              <div class="item-meta">
                <span v-if="r.matched_rules">命中规则：{{ r.matched_rules }}</span>
                <span v-if="r.rule_version">版本 {{ r.rule_version }}</span>
                <span class="item-time">{{ r.created_at }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { type ReportRecord, type MedicalRecordRecord, type PrescriptionRecord, type PaymentRecord, type TriageHistoryRecord, type BusinessError, triageRecordsApi } from '@aimedical/shared'

const router = useRouter()
const activeTab = ref('reports')

interface LoadState<T> {
  list: T[]
  loading: boolean
  error: string | null
  detail?: T | null
}

const reports = reactive<LoadState<ReportRecord>>({ list: [], loading: false, error: null, detail: null })
const medical = reactive<LoadState<MedicalRecordRecord>>({ list: [], loading: false, error: null, detail: null })
const prescriptions = reactive<LoadState<PrescriptionRecord>>({ list: [], loading: false, error: null, detail: null })
const payments = reactive<LoadState<PaymentRecord>>({ list: [], loading: false, error: null })
const triage = reactive<LoadState<TriageHistoryRecord>>({ list: [], loading: false, error: null })

const paymentFilters = reactive({
  dateRange: null as [Date, Date] | null,
  category: '',
})

const triageFilters = reactive({
  degradedOnly: false,
})

const totalAmount = computed(() =>
  payments.list.reduce((sum, p) => sum + p.amount, 0).toFixed(2)
)

// --- Mock data ---
const mockReports: ReportRecord[] = [
  {
    id: 1, type: '检验', name: '血常规', exam_date: '2026-06-20', report_date: '2026-06-20',
    doctor_name: '王主任', department_name: '神经内科', status: '已完成',
    summary: '白细胞计数轻度升高，中性粒细胞比例偏高，提示可能存在感染或炎症反应。其他指标均在正常范围内。建议结合临床症状进一步评估。',
    details: [
      { item: '白细胞 (WBC)', result: '12.5', reference_range: '4.0-10.0', flag: '↑', unit: '10⁹/L' },
      { item: '红细胞 (RBC)', result: '4.8', reference_range: '4.0-5.5', flag: '', unit: '10¹²/L' },
      { item: '血红蛋白 (Hb)', result: '140', reference_range: '120-160', flag: '', unit: 'g/L' },
      { item: '血小板 (PLT)', result: '220', reference_range: '100-300', flag: '', unit: '10⁹/L' },
    ],
  },
  {
    id: 2, type: '检查', name: '头颅 CT 平扫', exam_date: '2026-06-18', report_date: '2026-06-19',
    doctor_name: '张副主任', department_name: '影像科', status: '已完成',
    summary: '头颅 CT 平扫未见明显异常。脑实质密度均匀，脑室系统形态大小正常，中线结构居中。颅骨未见骨折征象。',
    details: [],
  },
  {
    id: 3, type: '检验', name: '生化全项', exam_date: '2026-06-15', report_date: '2026-06-16',
    status: '待审核',
    summary: '血糖5.8mmol/L（正常偏高），血脂各项在正常范围。肝肾功能未见明显异常。',
    details: [
      { item: '血糖 (GLU)', result: '5.8', reference_range: '3.9-6.1', flag: '↑', unit: 'mmol/L' },
      { item: '总胆固醇 (TC)', result: '4.5', reference_range: '3.0-5.7', flag: '', unit: 'mmol/L' },
      { item: '肌酐 (Cr)', result: '82', reference_range: '44-133', flag: '', unit: 'μmol/L' },
    ],
  },
]

const mockMedicalRecords: MedicalRecordRecord[] = [
  {
    id: 1, visit_date: '2026-06-20', department_name: '神经内科', doctor_name: '王主任',
    chief_complaint: '头痛3天，伴有恶心，无呕吐。疼痛位于前额部，呈搏动性，每次持续约2小时。',
    diagnosis: '偏头痛（初步诊断）',
    advice: '1. 注意休息，避免强光和噪音刺激。2. 规律作息，避免熬夜。3. 建议完善头颅CT检查。4. 如有加重随时复诊。',
  },
  {
    id: 2, visit_date: '2026-05-10', department_name: '普通内科', doctor_name: '李主治医师',
    chief_complaint: '发热2天，体温最高38.5°C，伴有咳嗽、咽痛。已自行服用对乙酰氨基酚。',
    diagnosis: '上呼吸道感染',
    advice: '1. 多饮水，注意休息。2. 继续物理降温。3. 按处方用药，出现呼吸困难及时就医。',
  },
]

const mockPrescriptions: PrescriptionRecord[] = [
  {
    id: 1, doctor_name: '王主任', department_name: '神经内科', diagnosis: '偏头痛',
    issue_date: '2026-06-20', status: '已审核',
    ai_review_summary: '处方审核通过。布洛芬剂量在推荐范围内，甲氧氯普胺与布洛芬无已知相互作用风险。注意：布洛芬可能引起胃肠道不适，建议饭后服用。',
    medications: [
      { drug_name: '布洛芬缓释胶囊', dosage: '300mg/粒', frequency: '每日2次', duration: '3天', quantity: 6, price: 2.5 },
      { drug_name: '盐酸甲氧氯普胺片', dosage: '5mg/片', frequency: '每日3次', duration: '3天', quantity: 9, price: 1.2 },
    ],
  },
  {
    id: 2, doctor_name: '李主治医师', department_name: '普通内科', diagnosis: '上呼吸道感染',
    issue_date: '2026-05-10', status: '已发药',
    ai_review_summary: '处方审核通过。头孢呋辛酯与氨酚黄那敏无显著相互作用。提醒：服用头孢类药物期间避免饮酒。',
    medications: [
      { drug_name: '头孢呋辛酯片', dosage: '250mg/片', frequency: '每日2次', duration: '5天', quantity: 10, price: 3.8 },
      { drug_name: '氨酚黄那敏胶囊', dosage: '10粒/盒', frequency: '每日3次', duration: '3天', quantity: 1, price: 12.0 },
    ],
  },
]

const mockPayments: PaymentRecord[] = [
  { id: 1, project_name: '神经内科门诊挂号', visit_date: '2026-06-20', amount: 15, status: '已缴费', payment_date: '2026-06-20', category: '挂号' },
  { id: 2, project_name: '头颅 CT 平扫', visit_date: '2026-06-20', amount: 260, status: '已缴费', payment_date: '2026-06-20', category: '检查' },
  { id: 3, project_name: '血常规+生化全项', visit_date: '2026-06-20', amount: 310, status: '已缴费', payment_date: '2026-06-20', category: '检验' },
  { id: 4, project_name: '布洛芬+甲氧氯普胺', visit_date: '2026-06-20', amount: 25.8, status: '已缴费', payment_date: '2026-06-20', category: '药费' },
  { id: 5, project_name: '内科门诊挂号', visit_date: '2026-05-10', amount: 15, status: '已缴费', payment_date: '2026-05-10', category: '挂号' },
  { id: 6, project_name: '中药代煎费', visit_date: '2026-04-20', amount: 30, status: '已退费', payment_date: '2026-04-21', category: '药费' },
]

// --- Load functions with mock delay ---
function simulateLoad<T>(target: LoadState<T>, mockData: T[]) {
  target.loading = true
  target.error = null
  target.detail = null
  setTimeout(() => {
    target.list = [...mockData]
    target.loading = false
  }, 400)
}

function loadReports() { simulateLoad(reports, mockReports) }
function loadMedical() { simulateLoad(medical, mockMedicalRecords) }
function loadPrescriptions() { simulateLoad(prescriptions, mockPrescriptions) }
function loadPayments() {
  payments.loading = true
  payments.error = null
  setTimeout(() => {
    let filtered = [...mockPayments]
    const { dateRange, category } = paymentFilters
    if (category) filtered = filtered.filter(p => p.category === category)
    if (dateRange?.length === 2) {
      const [start, end] = dateRange
      filtered = filtered.filter(p => {
        const d = p.visit_date || p.payment_date
        if (!d) return false
        const pd = new Date(d)
        return pd >= start && pd <= end
      })
    }
    payments.list = filtered
    payments.loading = false
  }, 400)
}

async function loadTriage() {
  triage.loading = true
  triage.error = null
  try {
    const params = triageFilters.degradedOnly ? { degraded: true } : undefined
    const result = await triageRecordsApi.list(params)
    if (!(result as BusinessError).isBusinessError) {
      triage.list = result as TriageHistoryRecord[]
    } else {
      triage.list = getMockTriageRecords()
    }
  } catch {
    triage.list = getMockTriageRecords()
  }
  triage.loading = false
}

function getMockTriageRecords(): TriageHistoryRecord[] {
  return [
    {
      id: 1, patient_id: 3, chief_complaint: '头痛3天，伴有恶心，前额搏动性疼痛',
      session_id: 'mock-session-001',
      recommended_departments: '神经内科,普通内科,中医科',
      recommended_doctors: '王主任,张副主任,李主治医师',
      is_degraded: false, rule_version: 'v1.0.0', rule_set_id: 'rule-set-neuro',
      matched_rules: '头痛规则-偏头痛,头痛规则-紧张性', created_at: '2026-06-29 14:30:00',
    },
    {
      id: 2, patient_id: 3, chief_complaint: '发烧2天，体温38.5°C，咳嗽咽痛',
      session_id: 'mock-session-002',
      recommended_departments: '呼吸内科,普通内科,感染科',
      recommended_doctors: '王主任,李主治医师',
      is_degraded: false, rule_version: 'v1.0.0', rule_set_id: 'rule-set-resp',
      matched_rules: '发热规则-上感,咳嗽规则', created_at: '2026-06-29 16:00:00',
    },
    {
      id: 3, patient_id: 3, chief_complaint: '腹痛1天，右下腹持续性疼痛',
      session_id: 'mock-degraded-001',
      recommended_departments: '普通内科', recommended_doctors: '张副主任',
      is_degraded: true, rule_version: 'v1.0.0', rule_set_id: 'rule-set-abd',
      matched_rules: '', created_at: '2026-06-29 10:15:00',
    },
  ]
}

function openReportDetail(r: ReportRecord) {
  reports.loading = true
  setTimeout(() => {
    reports.detail = r
    reports.loading = false
  }, 200)
}

function onTabChange(tab: string) {
  const loaders: Record<string, () => void> = {
    reports: loadReports,
    medical: loadMedical,
    prescriptions: loadPrescriptions,
    payments: loadPayments,
    triage: loadTriage,
  }
  loaders[tab]?.()
}

onMounted(loadReports)
</script>

<style scoped>
.records-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 { margin: 0; }

.tab-content { min-height: 300px; }

.loading-box { padding: 20px 0; }
.error-box { padding: 20px 0; display: flex; justify-content: center; }
.empty-box { padding: 40px 0; }

.report-item {
  padding: 12px 14px; border: 1px solid #ebeef5; border-radius: 8px; cursor: pointer;
  margin-bottom: 8px; transition: border-color 0.15s;
}
.report-item:hover { border-color: #409eff; background: #fafafa; }
.item-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.item-name { display: flex; align-items: center; gap: 8px; }
.item-title { font-size: 15px; font-weight: 500; }
.item-status { font-size: 13px; color: #909399; }
.item-sub { display: flex; gap: 16px; font-size: 13px; color: #909399; }

.detail-view { margin-top: 8px; }
.detail-card { margin-top: 10px; }
.detail-summary { margin-top: 12px; padding: 10px; background: #f5f7fa; border-radius: 8px; font-size: 14px; line-height: 1.6; color: #606266; }
.detail-table { margin-top: 14px; }
.detail-section { margin-top: 14px; }
.detail-section h4 { margin: 0 0 6px 0; font-size: 14px; }
.detail-section p { margin: 0; font-size: 14px; color: #606266; line-height: 1.6; }

.ai-review {
  margin-top: 16px; padding: 14px; background: #f0f9ff; border-radius: 8px; border: 1px solid #b3d8ff;
}
.ai-review-header { display: flex; align-items: center; gap: 6px; font-weight: 500; margin-bottom: 6px; }
.ai-icon { font-size: 18px; }
.ai-review-text { margin: 0; font-size: 13px; color: #606266; line-height: 1.5; }

.filter-bar {
  display: flex; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap;
}
.filter-total { margin-left: auto; font-size: 14px; color: #606266; }

.payment-item { padding: 12px 14px; border: 1px solid #ebeef5; border-radius: 8px; margin-bottom: 8px; }
.payment-left { display: flex; align-items: center; gap: 8px; }
.payment-right { display: flex; align-items: center; gap: 10px; }
.payment-amount { font-size: 16px; font-weight: 600; color: #f56c6c; }

.triage-item { padding: 12px 14px; border: 1px solid #ebeef5; border-radius: 8px; margin-bottom: 8px; }
.item-meta { display: flex; gap: 16px; margin-top: 6px; font-size: 12px; color: #c0c4cc; }
.item-time { margin-left: auto; }
</style>
