package com.ecommerce.project.aspect;

import com.ecommerce.project.annotation.Auditable;
import com.ecommerce.project.model.AuditLog;
import com.ecommerce.project.repositories.AuditLogRepository;
import com.ecommerce.project.security.services.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Audit Aspect for AOP-based Logging
 *
 * ============================================================
 * 🎓 AOP (ASPECT-ORIENTED PROGRAMMING):
 * ============================================================
 *
 * AOP allows you to add behavior to existing code without
 * modifying the code itself.
 *
 * KEY CONCEPTS:
 * - Aspect: The cross-cutting concern (audit logging)
 * - Advice: The action (log before/after method)
 * - Pointcut: Where to apply (methods with @Auditable)
 * - Join Point: The actual method execution
 *
 * BENEFITS:
 * - Clean separation of concerns
 * - No boilerplate in business logic
 * - Easy to add/remove auditing
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "I use AOP for cross-cutting concerns like logging and security.
 * The @Auditable annotation marks methods for auditing. The aspect
 * intercepts these methods and logs before/after execution."
 * ============================================================
 */
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Around advice for @Auditable methods
     *
     * @Around executes before and after the method
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        AuditLog auditLog = new AuditLog();

        // Get method details
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Set action and entity type
        auditLog.setAction(auditable.action());
        auditLog.setEntityType(auditable.entityType());

        // Get user details
        populateUserDetails(auditLog);

        // Get request details
        populateRequestDetails(auditLog);

        // Log request body if configured
        if (auditable.logRequestBody()) {
            try {
                String args = objectMapper.writeValueAsString(joinPoint.getArgs());
                auditLog.setDetails(truncate(args, 2000));
            } catch (Exception e) {
                auditLog.setDetails(Arrays.toString(joinPoint.getArgs()));
            }
        }

        // Execute the method
        Object result = null;
        try {
            result = joinPoint.proceed();
            auditLog.setStatus("SUCCESS");

            // Log response if configured
            if (auditable.logResponseBody() && result != null) {
                try {
                    String response = objectMapper.writeValueAsString(result);
                    auditLog.setNewValue(truncate(response, 2000));
                } catch (Exception e) {
                    auditLog.setNewValue(result.toString());
                }
            }

            // Try to extract entity ID from result
            extractEntityId(result, auditLog);

        } catch (Exception e) {
            auditLog.setStatus("FAILURE");
            auditLog.setDetails(e.getMessage());
            throw e;
        } finally {
            // Save audit log
            auditLogRepository.save(auditLog);
        }

        return result;
    }

    /**
     * Populate user details from security context
     */
    private void populateUserDetails(AuditLog auditLog) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (auth.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                auditLog.setUserId(userDetails.getId());
                auditLog.setUsername(userDetails.getUsername());
            } else {
                auditLog.setUsername(auth.getName());
            }
        } else {
            auditLog.setUsername("anonymous");
        }
    }

    /**
     * Populate HTTP request details
     */
    private void populateRequestDetails(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestUri(request.getRequestURI());
                auditLog.setHttpMethod(request.getMethod());
            }
        } catch (Exception e) {
            // Ignore - might not be in request context
        }
    }

    /**
     * Get client IP address (handles proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Try to extract entity ID from result
     */
    private void extractEntityId(Object result, AuditLog auditLog) {
        if (result == null) return;

        try {
            // Try to get id using reflection
            var idMethod = result.getClass().getMethod("getProductId");
            auditLog.setEntityId(String.valueOf(idMethod.invoke(result)));
        } catch (Exception e1) {
            try {
                var idMethod = result.getClass().getMethod("getCategoryId");
                auditLog.setEntityId(String.valueOf(idMethod.invoke(result)));
            } catch (Exception e2) {
                try {
                    var idMethod = result.getClass().getMethod("getOrderId");
                    auditLog.setEntityId(String.valueOf(idMethod.invoke(result)));
                } catch (Exception e3) {
                    // Unable to extract ID
                }
            }
        }
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
