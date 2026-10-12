package com.farlands.lifesteal;

import java.util.UUID;

public final class LifestealConstants {
    private LifestealConstants() {}

    public static final String MOD_ID = "farlands-lifesteal";
    public static final String NEOFORGE_MOD_ID = "farlands_lifesteal";
    public static final String MOD_NAME = "Farlands Lifesteal";

    public static final int DEFAULT_HEARTS = 10;
    public static final double DEFAULT_HEALTH = 20.0;
    public static final double MAX_HEALTH = 40.0;
    public static final int MAX_HEARTS = 20;
    public static final double MIN_HEALTH = 2.0;
    public static final double HEALTH_PER_HEART = 2.0;

    public static final int SPAWN_PROTECTION_SECONDS = 10;
    public static final int SPAWN_PROTECTION_TICKS = 200;
    public static final int SPAWN_PROTECTION_AMPLIFIER = 4;

    public static final String TAG_LIFESTEAL_ITEM = "LifestealItem";
    public static final String VALUE_HEART = "heart";
    public static final String VALUE_REVIVE_TOKEN = "revive_token";
    public static final String TAG_TARGET_UUID = "TargetUUID";
    public static final String TAG_TARGET_NAME = "TargetName";

    public static final UUID HEART_SKULL_UUID = UUID.fromString("88339911-2244-6688-9900-112233445576");
    public static final String HEART_SKULL_NAME = "Heart";
    public static final String HEART_TEXTURE_BASE64 = "ewogICJ0aW1lc3RhbXAiIDogMTc5MTM5NjU5OTM4OCwKICAicHJvZmlsZUlkIiA6ICI5MTU1ZmYzNTNlMzc0ZmZlYjE0MmE5NmU2MzU2ZjA4NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJlbG5hZFhCIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M4OTM5MDI5YWM0NGI1ZGM5Y2RiMWQ5ODhkODY0YmU4NTVjMmQwMTk0ZDhhNmViMmQzOGUyN2YxYWM0YWU1MjAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
}
