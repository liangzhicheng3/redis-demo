package com.liangzhicheng.modules.service.impl;

import com.liangzhicheng.modules.service.ISignTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class SignTimeServiceImpl implements ISignTimeService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 指定偏移量签到
     */
    public Boolean sign(Integer rangeId, Integer userId, long offset) {
        return setBit(generateSignKey(rangeId, userId), offset, Boolean.TRUE);
    }

    /**
     * 根据起始时间签到
     */
    public Boolean sign(Integer rangeId, Integer userId, LocalDateTime startTime) {
        return setBit(generateSignKey(rangeId, userId), intervalTime(startTime), Boolean.TRUE);
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
    public Boolean checkSign(Integer rangeId, Integer userId, LocalDateTime startTime) {
        return getBit(generateSignKey(rangeId, userId), intervalTime(startTime));
    }

    /**
     * 根据区间的id和用户id生成用户签到key
     */
    public String generateSignKey(Integer rangeId, Integer userId) {
        StringBuilder append = new StringBuilder("RangeId:Sign:");
        append.append(rangeId)
                .append(":")
                .append(userId);
        return append.toString();
    }

    /**
     * 获取当月连续签到次数
     */
    public long getContinuousSignCountOfMonth(Integer rangeId, Integer userId, LocalDateTime start) {
        int signCount = 0;
        List<Long> bitField = getBitField(generateSignKey(rangeId, userId), intervalTime(start) + 1, 0);
        if (!CollectionUtils.isEmpty(bitField)) {
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
     * 统计当前月份一共签到天数
     */
    public long getSignCountOfMonth(Integer rangeId, Integer userId) {
        return getBitCount(generateSignKey(rangeId, userId));
    }

    /**
     * 统计计数
     */
    public long getBitCount(String key) {
        return stringRedisTemplate.execute((RedisCallback<Long>) connection -> connection.bitCount(key.getBytes()));
    }

    /**
     * 获取多字节位域
     */
    public List<Long> getBitField(String key, int limit, long offset) {
        return stringRedisTemplate
                .opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(limit)).valueAt(offset));
    }

    /**
     * 获取当月签到日期
     */
    public Map<String, Boolean> getSignMonth(Integer rangeId, Integer userId, LocalDateTime startTime) {
        int days = intervalTime(startTime);
        Map<String, Boolean> signMap = new HashMap<>(days);
        List<Long> bitField = getBitField(generateSignKey(rangeId, userId), days + 1, 0);
        if(!CollectionUtils.isEmpty(bitField)){
            long signFlag = bitField.get(0) == null ? 0 : bitField.get(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for(int i = days; i >= 0; i--){
                LocalDateTime localDateTime = startTime.plusDays(i);
                signMap.put(localDateTime.format(formatter), signFlag >> 1 << 1 != signFlag);
                signFlag >>= 1;
            }
        }
        return signMap;
    }

    /**
     * 获取当前时间与起始时间的间隔天数
     */
    private int intervalTime(LocalDateTime start) {
        return (int) (LocalDateTime.now().toLocalDate().toEpochDay() - start.toLocalDate().toEpochDay());
    }

}