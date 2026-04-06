package com.example.simpletiktok.limit;

import com.example.simpletiktok.pojo.dto.VideoSaveDTO;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.UserHolder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class LimiterAop {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 基于 Redis 计数器做限流：
     * 1. 先对当前用户计数 +1；
     * 2. 首次计数时设置过期时间；
     * 3. 超过阈值直接返回错误响应，不继续执行业务方法。
     */
    @Around("@annotation(limit)")
    public Object restriction(ProceedingJoinPoint joinPoint, Limit limit) throws Throwable {
        User current = UserHolder.get();
        if (current == null) {
            return joinPoint.proceed();
        }

        // 编辑已有视频不计入“上传次数”，仅新发布视频参与限流
        if (isEditVideoSave(joinPoint.getArgs())) {
            return joinPoint.proceed();
        }

        String key = limit.key() + current.getId();
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(limit.time()));
        }
        if (count != null && count > limit.limit()) {
            return R.error().message(limit.msg());
        }
        return joinPoint.proceed();
    }

    /**
     * 识别 /api/video/save 请求是否属于“编辑”：
     * dto.id 不为空表示编辑，不走上传限流。
     */
    private boolean isEditVideoSave(Object[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        for (Object arg : args) {
            if (arg instanceof VideoSaveDTO dto) {
                return dto.getId() != null;
            }
        }
        return false;
    }
}
