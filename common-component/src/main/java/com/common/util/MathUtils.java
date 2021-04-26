package com.common.util;

public class MathUtils {

    public static int max(int a, int b, int... more) {
        int max = Math.max(a, b);
        if (more != null) {
            for (int i : more) {
                max = Math.max(max, i);
            }
        }
        return max;
    }
}
