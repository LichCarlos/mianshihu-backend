package com.carlos.mianshihu.constant;

public interface RedisConstant {
    String  USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    static  String getUserSignInRedisKey( int year,long userId){
        return String.format("%s:%s:%S", USER_SIGN_IN_REDIS_KEY_PREFIX, year,userId);
    }
}
