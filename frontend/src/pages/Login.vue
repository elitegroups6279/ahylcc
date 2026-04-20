<template>
  <div class="login-container">
    <!-- 浮动装饰光圈 -->
    <div class="floating-orb orb-1"></div>
    <div class="floating-orb orb-2"></div>
    <div class="floating-orb orb-3"></div>
    <div class="floating-orb orb-4"></div>
    
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon-wrapper">
          <el-icon :size="32" color="#fff">
            <Monitor />
          </el-icon>
        </div>
        <h1 class="login-title">智慧养老管理系统</h1>
        <p class="login-subtitle">Smart Elderly Care Management System</p>
      </div>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="onLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="onLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      

    </div>
    
    <div class="login-footer">
      <p class="company-info">安庆市泰呈健康评估服务有限公司 版权所有</p>
      <p class="icp-info"><a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" style="color: inherit; text-decoration: none;">皖ICP备2022009803号-4</a></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useAuthStore } from '../store/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Monitor } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()

const form = reactive({
  username: 'admin',
  password: 'Admin123!'
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const loading = ref(false)

async function onLogin() {
  if (!formRef.value) return
  
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
  } catch (e) {
    return
  }
  
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

/* 浮动光圈装饰 */
.floating-orb {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.3) 0%, rgba(24, 144, 255, 0) 70%);
  pointer-events: none;
}

.orb-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation: float 8s ease-in-out infinite;
}

.orb-2 {
  width: 200px;
  height: 200px;
  top: 20%;
  right: -50px;
  animation: float 10s ease-in-out infinite 1s;
  background: radial-gradient(circle, rgba(54, 207, 201, 0.25) 0%, rgba(54, 207, 201, 0) 70%);
}

.orb-3 {
  width: 150px;
  height: 150px;
  bottom: 20%;
  left: 10%;
  animation: float 7s ease-in-out infinite 0.5s;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.2) 0%, rgba(24, 144, 255, 0) 70%);
}

.orb-4 {
  width: 250px;
  height: 250px;
  bottom: -80px;
  right: 10%;
  animation: float 9s ease-in-out infinite 2s;
  background: radial-gradient(circle, rgba(54, 207, 201, 0.2) 0%, rgba(54, 207, 201, 0) 70%);
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-30px) scale(1.05);
  }
}

.login-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 40px;
  max-width: 420px;
  width: 90%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon-wrapper {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.3), rgba(54, 207, 201, 0.3));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.login-title {
  font-size: 26px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.login-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  letter-spacing: 1px;
}

.login-form {
  margin-bottom: 20px;
}

/* 输入框深色半透明风格 */
.login-card :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: none !important;
}

.login-card :deep(.el-input__inner) {
  color: #fff;
  height: 44px;
}

.login-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.login-card :deep(.el-input__prefix .el-icon) {
  color: rgba(255, 255, 255, 0.5);
}

.login-card :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.3);
}

.login-card :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(24, 144, 255, 0.6);
}

/* 登录按钮科技蓝渐变 */
.login-btn {
  width: 100%;
  height: 44px;
  background: linear-gradient(135deg, #1890ff, #36cfc9);
  border: none;
  border-radius: 8px;
  font-size: 16px;
  letter-spacing: 2px;
  transition: all 0.3s;
  margin-top: 8px;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(24, 144, 255, 0.4);
}

/* 底部信息区 */
.login-footer {
  position: absolute;
  bottom: 30px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 1;
}

.company-info {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 4px 0;
}

.icp-info {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  margin: 0;
}
</style>
