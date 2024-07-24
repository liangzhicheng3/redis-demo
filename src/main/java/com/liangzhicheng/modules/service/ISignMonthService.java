package com.liangzhicheng.modules.service;

import java.util.Map;

public interface ISignMonthService {

    Boolean sign(String userId);

    Boolean getBit(String key, long offset);

    Boolean setBit(String key, long offset, boolean tag);

    int checkSign(String userId);

    long getContinuousSignCountOfPeriod(String userId, Integer period);

    long getSignCountOfMonth(String userId);

    Map<String, Boolean> getSignMonth(String userId);

}