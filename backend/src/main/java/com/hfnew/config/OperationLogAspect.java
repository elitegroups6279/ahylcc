package com.hfnew.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfnew.entity.OperationLog;
import com.hfnew.security.AuthUserPrincipal;
import com.hfnew.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /**
     * 定义切点：标注了 @OpLog 注解的方法
     */
    @Pointcut("@annotation(com.hfnew.config.OpLog)")
    public void logPointcut() {
    }

    /**
     * 环绕通知
     */
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OpLog opLog = method.getAnnotation(OpLog.class);

        // 获取请求信息
        HttpServletRequest request = getRequest();
        String ip = getClientIp(request);

        // 获取当前用户
        AuthUserPrincipal user = getCurrentUser();

        // 获取请求参数
        String params = getParams(joinPoint);

        // 构建日志实体
        OperationLog logEntity = new OperationLog();
        logEntity.setModule(opLog.module());
        logEntity.setOperation(opLog.operation());
        logEntity.setMethod(request != null ? request.getMethod() + " " + request.getRequestURI() : signature.toString());
        logEntity.setParams(params);
        logEntity.setIp(ip);
        logEntity.setOperationTime(LocalDateTime.now());

        if (user != null) {
            logEntity.setUserId(user.getUserId());
            logEntity.setUsername(user.getUsername());
        }

        Object result = null;
        try {
            // 执行原方法
            result = joinPoint.proceed();
            logEntity.setStatus(1); // 成功
        } catch (Throwable e) {
            logEntity.setStatus(0); // 失败
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            // 异步保存日志
            operationLogService.saveLogAsync(logEntity);
        }

        return result;
    }

    /**
     * 获取当前登录用户
     */
    private AuthUserPrincipal getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthUserPrincipal) {
                return (AuthUserPrincipal) auth.getPrincipal();
            }
        } catch (Exception e) {
            log.warn("获取当前用户失败", e);
        }
        return null;
    }

    /**
     * 获取请求对象
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个IP时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 获取请求参数
     */
    private String getParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "[]";
            }

            // 过滤敏感参数
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }

                Object arg = args[i];
                if (arg == null) {
                    sb.append("null");
                } else if (arg instanceof HttpServletRequest) {
                    sb.append("HttpServletRequest");
                } else {
                    String json = objectMapper.writeValueAsString(arg);
                    // 限制长度
                    if (json.length() > 500) {
                        json = json.substring(0, 500) + "...";
                    }
                    sb.append(json);
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            log.warn("序列化请求参数失败", e);
            return "[]";
        }
    }
}
