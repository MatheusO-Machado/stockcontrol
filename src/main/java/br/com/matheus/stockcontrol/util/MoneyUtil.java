package br.com.matheus.stockcontrol.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {
    private MoneyUtil() {}

    public static int toCents(BigDecimal value) {
        if (value == null) return 0;
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        return scaled.movePointRight(2).intValueExact();
    }

    public static BigDecimal fromCents(int cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2);
    }
}