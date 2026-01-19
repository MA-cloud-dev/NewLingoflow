package com.lingoflow.util;

/**
 * SM-2 间隔重复算法实现
 * 
 * quality 评分:
 * - 5: 完美记忆 (认识+测试正确)
 * - 0: 完全忘记 (不认识 或 认识+测试错误)
 */
public class SM2Algorithm {

    private static final float MIN_EF = 1.3f;
    private static final float DEFAULT_EF = 2.5f;

    /**
     * 计算下次复习间隔
     * 
     * @param currentInterval 当前间隔天数
     * @param currentEF       当前简易因子
     * @param quality         回答质量 (0-5)
     * @return SM2Result 包含新间隔和新EF
     */
    public static SM2Result calculate(int currentInterval, float currentEF, int quality) {
        float newEF;
        int newInterval;
        int newFamiliarity;

        if (quality >= 3) {
            // 正确回答
            newEF = currentEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f));
            newEF = Math.max(newEF, MIN_EF);

            if (currentInterval == 0) {
                newInterval = 1;
            } else if (currentInterval == 1) {
                newInterval = 6;
            } else {
                newInterval = Math.round(currentInterval * newEF);
            }

            // 熟悉度增加
            newFamiliarity = Math.min(100, 15);
        } else {
            // 错误回答：重置间隔
            newEF = Math.max(currentEF - 0.2f, MIN_EF);
            newInterval = 1;

            // 熟悉度降低
            newFamiliarity = -20;
        }

        return new SM2Result(newInterval, newEF, newFamiliarity);
    }

    /**
     * SM-2 计算结果
     */
    public static class SM2Result {
        private final int intervalDays;
        private final float easinessFactor;
        private final int familiarityDelta; // 熟悉度变化值

        public SM2Result(int intervalDays, float easinessFactor, int familiarityDelta) {
            this.intervalDays = intervalDays;
            this.easinessFactor = easinessFactor;
            this.familiarityDelta = familiarityDelta;
        }

        public int getIntervalDays() {
            return intervalDays;
        }

        public float getEasinessFactor() {
            return easinessFactor;
        }

        public int getFamiliarityDelta() {
            return familiarityDelta;
        }
    }
}
