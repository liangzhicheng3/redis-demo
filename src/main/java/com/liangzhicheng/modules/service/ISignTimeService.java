package com.liangzhicheng.modules.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface ISignTimeService {

    Boolean sign(Integer rangeId, Integer userId, long offset);

    Boolean checkSign(Integer rangeId, Integer userId, LocalDateTime start);

    Boolean sign(Integer rangeId, Integer userId, LocalDateTime start);

    long getContinuousSignCountOfMonth(Integer rangeId, Integer userId, LocalDateTime start);

    long getSignCountOfMonth(Integer rangeId, Integer userId);

    Map<String, Boolean> getSignMonth(Integer rangeId, Integer userId, LocalDateTime start);

}