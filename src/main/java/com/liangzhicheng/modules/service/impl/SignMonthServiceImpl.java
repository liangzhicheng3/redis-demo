package com.liangzhicheng.modules.service.impl;

import com.liangzhicheng.modules.service.ISignMonthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class SignMonthServiceImpl implements ISignMonthService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 签到
     */
    public Boolean sign(String userId) {
        Boolean signFlag = Boolean.FALSE;
        if(Boolean.TRUE.equals(setBit(generateSignKey(userId), (long) dayOfMonth() - 1, Boolean.TRUE))){
            signFlag = Boolean.TRUE;
        }
        //获取连续签到次数，最大连续次数为7天
        getContinuousSignCountOfPeriod(userId, 7);
        return signFlag;
    }

    /**
     * 获取标记位
     */
    public Boolean getBit(String key, long offset) {
        return stringRedisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 设置标记位
     */
    public Boolean setBit(String key, long offset, boolean tag) {
        return stringRedisTemplate.opsForValue().setBit(key, offset, tag);
    }

    /**
     * 检查今天是否签到
     */
    public int checkSign(String userId) {
        return getBit(generateSignKey(userId), new DateTime().getDayOfMonth() - 1) ? 1 : 0;
    }

    /**
     * 根据月份和用户id生成用户签到key->UserId:Sign:100:2024-07
     */
    public String generateSignKey(String userId) {
        DateTime dateTime = new DateTime();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM");
        StringBuilder append = new StringBuilder("UserId:Sign:");
        append.append(userId)
                .append(":")
                .append(dateTime.toString(formatter));
        return append.toString();
    }

    /**
     * 以7天为一个周期连续签到次数
     */
    public long getContinuousSignCountOfPeriod(String userId, Integer period) {
        //获取当前连续签到次数
        long count = getContinuousSignCountOfMonth(userId);
        //按最大连续签到取余
        if(period != null && period < count){
            long num = count % period;
            if(num == 0){
                count = period;
            }else{
                count = num;
            }
        }
        return count;
    }

    /**
     * 获取当月连续签到次数
     */
    public long getContinuousSignCountOfMonth(String userId) {
        int signCount = 0;
        List<Long> bitField = getBitField(generateSignKey(userId), dayOfMonth(), 0);
        if(!CollectionUtils.isEmpty(bitField)){
            long signFlag = bitField.get(0) == null ? 0 : bitField.get(0);
            DateTime dateTime = new DateTime();
            //连续不为0，即为连续签到次数，当天未签到情况下
            for(int i = 0; i < dateTime.getDayOfMonth(); i++){
                if(signFlag >> 1 << 1 == signFlag){
                    if(i > 0){
                        break;
                    }
                }else{
                    signCount += 1;
                }
                signFlag >>= 1;
            }
        }
        return signCount;
    }

    /**
     * 获取多字节位域
     */
    public List<Long> getBitField(String buildSignKey, int limit, long offset) {
        return stringRedisTemplate
                .opsForValue()
                .bitField(buildSignKey, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(limit)).valueAt(offset));
    }

    /**
     * 统计当前月份一共签到天数
     */
    public long getSignCountOfMonth(String userId) {
        return getBitCount(generateSignKey(userId));
    }

    /**
     * 统计计数
     */
    public long getBitCount(String key) {
        return stringRedisTemplate.execute((RedisCallback<Long>) connection -> connection.bitCount(key.getBytes()));
    }

    /**
     * 获取当月签到日期
     */
    public Map<String, Boolean> getSignMonth(String userId) {
        DateTime newDateTime = new DateTime();
        int monthValue = newDateTime.dayOfMonth().getMaximumValue();
        Map<String, Boolean> signMap = new HashMap<>(newDateTime.getDayOfMonth());
        List<Long> bitField = getBitField(generateSignKey(userId), monthValue, 0);
        if(!CollectionUtils.isEmpty(bitField)){
            long signFlag = bitField.get(0) == null ? 0 : bitField.get(0);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
            for(int i = monthValue; i > 0; i--){
                DateTime dateTime = newDateTime.withDayOfMonth(i);
                signMap.put(dateTime.toString(formatter), signFlag >> 1 << 1 != signFlag);
                signFlag >>= 1;
            }
        }
        return signMap;
    }

    private int dayOfMonth() {
        DateTime dateTime = new DateTime();
        return dateTime.dayOfMonth().get();
    }

}