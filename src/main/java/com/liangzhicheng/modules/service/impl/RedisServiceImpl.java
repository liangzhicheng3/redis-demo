package com.liangzhicheng.modules.service.impl;

import com.liangzhicheng.modules.service.IRedisService;
import com.liangzhicheng.modules.service.ISignMonthService;
import com.liangzhicheng.modules.service.ISignTimeService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
@Service
public class RedisServiceImpl implements IRedisService {

    private static final String KEY_LOTTERY_PREFIX = "lottery:";
    private static final String KEY_PRAISE_ARTICLE_PREFIX = "praise:article:";
    private static final String KEY_RANKING = "ranking";
    private static final String KEY_VISITOR_PAGE_PREFIX  = "visitor:page:";
    private static final String KEY_GEO_DRINKS_PREFIX = "geo:drinks";

    private static final Integer TIME = 5; //单位时间（秒）
    private static final Integer MAX = 100; //允许访问上限次数

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private final ISignMonthService signMonthService;
    private final ISignTimeService signTimeService;

    @Override
    public void lottery() {
        Integer lotteryId = 1;
        Integer[] userIds = {100, 101, 102, 103, 104, 105, 106};
        Long num = 2L; //幸运用户个数
        add(lotteryId, userIds);
        List<Object> list = lucky(lotteryId, num);
        log.info("活动【{}】，幸运中奖用户【{}】", lotteryId, list);
    }

    private void add(Integer lotteryId,
                     Integer ... userIds){
        String key = KEY_LOTTERY_PREFIX + lotteryId;
        redisTemplate.opsForSet().add(key, userIds);
    }

    private List<Object> lucky(Integer lotteryId,
                               Long num){
        String key = KEY_LOTTERY_PREFIX + lotteryId;
        //随机抽取，抽完后将用户移除奖池
        List<Object> list = redisTemplate.opsForSet().pop(key, num);
        //随机抽取，抽完后将用户保存在奖池
//        List<Object> list = redisTemplate.opsForSet().randomMembers(key, num);
        return list;
    }

    @Override
    public void praise() {
        Integer articleId = 1;
        Integer[] userIds = {100, 101, 102, 103, 104, 105, 106};
        Long likeNum = like(articleId, userIds);
        unLike(articleId, 103);
        likeNum = likeNum(articleId);
        Boolean b103 = isLike(articleId, 103);
        Boolean b104 = isLike(articleId, 104);
        log.info("文章【{}】，点赞数量【{}】，用户103的点赞状态【{}】，用户104的点赞状态【{}】", articleId, likeNum, b103, b104);
    }

    private Long like(Integer articleId,
                      Integer ... userIds){
        String key = KEY_PRAISE_ARTICLE_PREFIX + articleId;
        return redisTemplate.opsForSet().add(key, userIds);
    }

    private Long unLike(Integer articleId,
                        Integer ... userIds){
        String key = KEY_PRAISE_ARTICLE_PREFIX + articleId;
        return redisTemplate.opsForSet().remove(key, userIds);
    }

    private Long likeNum(Integer articleId){
        String key = KEY_PRAISE_ARTICLE_PREFIX + articleId;
        return redisTemplate.opsForSet().size(key);
    }

    private Boolean isLike(Integer articleId,
                           Integer userId){
        String key = KEY_PRAISE_ARTICLE_PREFIX + articleId;
        return redisTemplate.opsForSet().isMember(key, userId);
    }

    @Override
    public void ranking() {
        add(100, (double) 60);
        add(101, (double) 80);
        add(102, (double) 100);
        add(103, (double) 90);
        add(104, (double) 70);
        add(105, (double) 96);
        add(106, (double) 88);
        Set<ZSetOperations.TypedTuple<Object>> range = range(0, -1);
        log.info("所有用户排行【{}】", range);
        range = range(0, 2);
        range.forEach(typedTuple -> {
            Object value = typedTuple.getValue();
            Double score = typedTuple.getScore();
            log.info("用户【{}】，分数【{}】", value, score);
        });
    }

    private Boolean add(Integer userId,
                        Double score){
        return redisTemplate.opsForZSet().add(KEY_RANKING, userId, score);
    }

    private Set<ZSetOperations.TypedTuple<Object>> range(long min,
                                                         long max){
        //降序
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(KEY_RANKING, min, max);
        //升序
//        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().rangeWithScores(KEY_RANKING, min, max);
        return set;
    }

    @Override
    public void visitor() { //不精准的去重计数
        Integer pageId = 1; //页面id
        Integer i1 = 0; //模拟10000个用户访问id为1的页面
        do{
            i1++;
            visitor(pageId, i1);
        }while(i1 < 10000);
        Integer i2 = 0;
        do{
            i2++;
            visitor(pageId, i2);
        }while(i2 < 10000);
        Long num = getVisitor(pageId);
        log.info("页面【{}】，访问数【{}】", pageId, num);
    }

    private Long visitor(Integer pageId,
                         Integer userId){
        String key = KEY_VISITOR_PAGE_PREFIX + pageId;
        return redisTemplate.opsForHyperLogLog().add(key, userId);
    }

    private Long getVisitor(Integer pageId){
        String key = KEY_VISITOR_PAGE_PREFIX + pageId;
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    @Override
    public void signMonth() {
        //模拟用户签到
        signMonthService.setBit("UserId:Sign:100:2024-07", 0, Boolean.TRUE);
        log.info("100用户今日是否已签到【{}】", signMonthService.checkSign("100"));
        log.info("目前连续签到【{}】天", signMonthService.getContinuousSignCountOfPeriod("100", 7));
        log.info("本月一共签到【{}】天", signMonthService.getSignCountOfMonth("100"));
        log.info("本月签到详情：");
        for(Map.Entry<String, Boolean> entry : signMonthService.getSignMonth("100").entrySet()){
            log.info("【{}】：【{}】", entry.getKey(), entry.getValue() ? "√" : "-");
        }
    }

    @Override
    public void signTime() {
        LocalDateTime startTime = LocalDateTime.of(2024, 7, 1, 1, 0, 0);
//        startTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Integer rangeId = 1;
        Integer userId = 8899;
        log.info("签到开始时间【{}】", startTime.format(DateTimeFormatter.ISO_DATE_TIME));
        log.info("活动【{}】，用户【{}】", rangeId, userId);
        //指定偏移量签到
        signTimeService.sign(rangeId, userId, 0);
        log.info("今天是否签到【{}】", signTimeService.checkSign(rangeId, userId, startTime) ? "√" : "-");
        log.info("签到操作之前的签到状态【{}】（-：表示今日第一次签到，√：表示今天已经签到过了）", signTimeService.sign(rangeId, userId, startTime) ? "√" : "-");
        log.info("连续签到【{}】天", signTimeService.getContinuousSignCountOfMonth(rangeId, userId, startTime));
        log.info("总共签到【{}】天", signTimeService.getSignCountOfMonth(rangeId, userId));
        for(Map.Entry<String, Boolean> entry : signTimeService.getSignMonth(rangeId, userId, startTime).entrySet()) {
            log.info("签到详情>【{}】：【{}】", entry.getKey(), entry.getValue() ? "√" : "-");
        }
    }

    @Override
    public void geo() {
        add("starbucks", new Point(116.62445, 39.86206));
        add("yidiandian", new Point(117.3514785, 38.7501247));
        add("xicha", new Point(116.538542, 39.75412));
        get("starbucks", "yidiandian", "xicha");
        GeoResults positionByPoint = getPositionByPoint(new Point(116, 39), new Distance(120, Metrics.KILOMETERS));
        List<GeoResult> contentList = positionByPoint.getContent();
        log.info("根据坐标获取指定范围位置：");
        Optional.ofNullable(contentList)
                .orElse(new ArrayList<>())
                .forEach(geoResult -> {
                    log.info("【{}】", geoResult.getContent());
                });
        GeoResults positionByPlace = getPositionByPlace("starbucks", new Distance(120, Metrics.KILOMETERS));
        contentList = positionByPlace.getContent();
        log.info("根据一个位置获取指定范围内其他位置：");
        Optional.ofNullable(contentList)
                .orElse(new ArrayList<>())
                .forEach(geoResult -> {
                    log.info("【{}】", geoResult.getContent());
                });
        getGeoHash("starbucks", "yidiandian", "xicha");
        del("yidiandian", "xicha");
    }

    private void add(String name,
                     Point point){
        Long add = redisTemplate.opsForGeo().add(KEY_GEO_DRINKS_PREFIX, point, name);
        log.info("添加名称【{}】，坐标信息【{}】，坐标信息数量【{}】", name, point, add);
    }

    private List<Point> get(String ... names) {
        List<Point> positionList = redisTemplate.opsForGeo().position(KEY_GEO_DRINKS_PREFIX, names);
        log.info("获取名称【{}】，坐标信息【{}】", names, positionList);
        return positionList;
    }

    private void del(String ... names) {
        List<Point> positionList = get(names);
        Long remove = redisTemplate.opsForGeo().remove(KEY_GEO_DRINKS_PREFIX, names);
        log.info("删除名称【{}】，坐标信息【{}】，坐标信息数量【{}】", names, positionList, remove);
    }

    /**
     * 根据坐标获取指定范围位置
     */
    private GeoResults getPositionByPoint(Point point,
                                          Distance distance) {
        Circle circle = new Circle(point, distance);
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands
                .GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance() //包含距离
                .includeCoordinates() //包含坐标
                .sortAscending() //排序，还可选sortDescending()
                .limit(5); //获取前多少个
        GeoResults geoResults = redisTemplate.opsForGeo().radius(KEY_GEO_DRINKS_PREFIX, circle, args);
        log.info("根据坐标获取【{}】【{}】，范围数据【{}】", point, distance, geoResults);
        return geoResults;
    }

    /**
     * 根据一个位置获取指定范围内其他位置
     */
    private GeoResults getPositionByPlace(String name,
                                          Distance distance) {
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands
                .GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(5);
        GeoResults geoResults = redisTemplate.opsForGeo().radius(KEY_GEO_DRINKS_PREFIX, name, distance, args);
        log.info("根据名称位置【{}】，获取【{}】，范围数据【{}】", name, distance, geoResults);
        return geoResults;
    }

    /**
     * 获取Geo Hash
     */
    private List<String> getGeoHash(String ... names) {
        List<String> hash = redisTemplate.opsForGeo().hash(KEY_GEO_DRINKS_PREFIX, names);
        log.info("名称【{}】，对应hash【{}】", names, hash);
        return hash;
    }

    @SneakyThrows
    @Override
    public void limit() {
        String userName = "user1";
        int num = 1;
        Boolean limitFlag = limit(userName);
        log.info("第【{}】次是否放行【{}】", num, limitFlag);
        for(int i = 0; i < 100; i++){
            num += 1;
            limit(userName);
        }
        limitFlag = limit(userName);
        log.info("第【{}】次是否放行【{}】", num, limitFlag);
        Thread.sleep(5000);
        limitFlag = limit(userName);
        log.info("模拟等待5s后，第【{}】次是否放行【{}】", num, limitFlag);
    }

    /**
     * 校验访问频率
     * @param uniqueId 用于限流的唯一id，可以是用户id或者客户端ip，等
     * @return true：放行，false：拦截
     */
    private Boolean limit(String uniqueId) {
        String key = "r:q:" + uniqueId;
        Long increment = redisTemplate.opsForValue().increment(key);
        if(increment == 1){
            redisTemplate.expire(key, TIME, TimeUnit.SECONDS);
        }
        if(increment <= MAX){
            return true;
        }
        return false;
    }

}