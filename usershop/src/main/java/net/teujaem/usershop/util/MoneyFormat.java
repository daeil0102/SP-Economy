package net.teujaem.usershop.util;

public final class MoneyFormat {

    private MoneyFormat() {
    }

    /** 1000 -> "1,000" */
    public static String format(long amount) {
        return String.format("%,d", amount);
    }

    /** 남은 시간(ms)을 "1일 2시 3분" 형태로 변환. 1분 미만은 "1분 미만"으로 표기. */
    public static String remainingTime(long millis) {
        if (millis <= 0) {
            return "만료됨";
        }
        long totalMinutes = millis / 60000L;
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("일 ");
        if (days > 0 || hours > 0) sb.append(hours).append("시 ");
        sb.append(minutes).append("분");
        return sb.toString();
    }
}
