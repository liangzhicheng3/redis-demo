package com.liangzhicheng.modules.controller;

import com.liangzhicheng.modules.service.IRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/redis")
public class RedisController {

    private final IRedisService redisService;

    /**
     * 抽奖
     */
    @GetMapping(value = "/lottery")
    public void lottery(){
        redisService.lottery();
    }

    /**
     * 点赞/收藏
     */
    @GetMapping(value = "/praise")
    public void praise(){
        redisService.praise();
    }

    /**
     * 排行榜
     */
    @GetMapping(value = "/ranking")
    public void ranking(){
        redisService.ranking();
    }

    /**
     * 访客
     */
    @GetMapping(value = "/visitor")
    public void visitor(){
        redisService.visitor();
    }

    /**
     * 按月签到
     */
    @GetMapping(value = "/sign-month")
    public void signMonth(){
        redisService.signMonth();
    }

    /**
     * 按时间签到
     */
    @GetMapping(value = "/sign-time")
    public void signTime(){
        redisService.signTime();
    }

    /**
     * 搜索附近
     */
    @GetMapping(value = "/geo")
    public void geo(){
        redisService.geo();
    }

    /**
     * 限流
     */
    @GetMapping(value = "/limit")
    public void limit(){
        redisService.limit();
    }

    //============================== Redis中Spring Session，实现Session共享 ==============================
    /**
     * 开启两个服务，分别监听8080和8081，8080调用赋值接口，8081调用获取接口，实现两个服务共享了一份Session数据
     */
    /**
     * 设置session值
     */
    @GetMapping(value = "/session-set")
    public Map<String, String> sessionSet(HttpServletRequest request) {
        String key = "key";
        String value = "value";
        Map<String, String> map = new HashMap<>();
        map.put("id", request.getSession().getId());
        map.put(key, value);
        //自定义session值
        request.getSession().setAttribute(key, value);
        return map;
    }

    /**
     * 获取session值
     */
    @GetMapping(value = "/session-get")
    public Map<String, Object> sessionGet(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        //遍历所有session值
        Enumeration<String> attributeNames = request.getSession().getAttributeNames();
        while(attributeNames.hasMoreElements()){
            String k = attributeNames.nextElement();
            map.put(k, request.getSession().getAttribute(k));
        }
        map.put("id", request.getSession().getId());
        return map;
    }

}