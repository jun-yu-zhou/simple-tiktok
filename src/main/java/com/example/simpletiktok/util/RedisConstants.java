package com.example.simpletiktok.util;

public interface RedisConstants {
    String EMAIL_CODE = "email:code:";
    long EMAIL_CODE_TTL = 300L;
    String USER_SEARCH_HISTORY = "user:search:history:";
    long USER_SEARCH_HISTORY_TTL = 432000L;
    String USER_HISTORY_VIDEO = "user:history:video:";
    String USER_HISTORY_BLOOM = "user:history:bloom:";
    long HISTORY_TIME = 432000L;
    // 关注流/分享流收件箱过期时间：7天
    long FEED_INBOX_TTL = 604800L;
    // 关注流发件箱过期时间：30天
    long FEED_OUTBOX_TTL = 2592000L;
    String USER_MODEL = "user:model:";
    String HOT_RANK = "hot:rank";
    String HOT_VIDEO = "hot:video:";
    String SYSTEM_STOCK = "system:stock:";
    String SYSTEM_TYPE_STOCK = "system:type:stock:";
    String OUT_FOLLOW = "out:follow:feed:";
    String IN_FOLLOW = "in:follow:feed:";
    String IN_FRIEND_SHARE = "in:friend:share:";
    String USER_FOLLOW = "user:follow:";
    String USER_FANS = "user:fans:";
    // 发布视频限流计数器前缀
    String VIDEO_UPLOAD_LIMIT = "video:upload:limit:";
}
