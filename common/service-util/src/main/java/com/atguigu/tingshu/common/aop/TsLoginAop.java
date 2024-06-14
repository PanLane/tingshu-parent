package com.atguigu.tingshu.common.aop;

import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Aspect
public class TsLoginAop {

@Autowired
private RedisTemplate redisTemplate;

@Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(tsLogin)")
public Object around(ProceedingJoinPoint joinPoint, TsLogin tsLogin) throws Throwable {
    //从请求头中获取token
    ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = requestAttributes.getRequest();//获取request对象
    String token = request.getHeader("token");

        UserInfo user = null;
        //判断是否需要登录
        if(tsLogin.required()){
            //需要登录，检验token
            if(!StringUtils.hasText(token)) throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);//判空
            //从redis中根据token获取用户信息
            user = (UserInfo) redisTemplate.opsForValue().get(token);
            if(user==null) throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        //将用户信息于当前线程进行绑定
        try {
            if(user!=null){
                AuthContextHolder.setUserId(user.getId());
            }else if(StringUtils.hasText(token)) {
                user = (UserInfo) redisTemplate.opsForValue().get(token);
                if(user!=null) AuthContextHolder.setUserId(user.getId());
            }
            //放行
            return joinPoint.proceed();
        } finally {
            //释放ThreadLocal，防止内存泄露
            AuthContextHolder.removeUserId();
        }
    }
}
