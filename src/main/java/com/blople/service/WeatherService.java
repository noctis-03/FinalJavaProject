package com.blople.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * 날씨 조회 서비스 (Open-Meteo, API 키 불필요).
 */
public class WeatherService {

    public static final double DAEJEON_LAT = 36.3504;
    public static final double DAEJEON_LON = 127.3845;

    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS    = 4000;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "weather-service");
                t.setDaemon(true);
                return t;
            });

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public void fetchAsync(double lat, double lon, Consumer<WeatherSnapshot> onResult) {
        executor.submit(() -> {
            try {
                onResult.accept(fetch(lat, lon));
            } catch (Exception e) {
                onResult.accept(WeatherSnapshot.error(e.getMessage()));
            }
        });
    }

    public void fetchDaejeonAsync(Consumer<WeatherSnapshot> onResult) {
        fetchAsync(DAEJEON_LAT, DAEJEON_LON, onResult);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // ============================================================
    //  호출
    // ============================================================
    private WeatherSnapshot fetch(double lat, double lon) throws Exception {
        String key = lat + "," + lon;
        CacheEntry cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
            return cached.snapshot;
        }

        String url = String.format(java.util.Locale.US,
                "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=%.4f&longitude=%.4f" +
                        "&current=temperature_2m,weather_code" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                        "&timezone=Asia%%2FSeoul&forecast_days=2",
                lat, lon);

        String body = httpGet(url);
        WeatherSnapshot snap = parse(body);
        cache.put(key, new CacheEntry(snap, now));
        return snap;
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("HTTP " + code);
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } finally {
            conn.disconnect();
        }
    }

    // ============================================================
    //  파싱 — daily / current 섹션을 명시적으로 분리해서 읽는다
    // ============================================================
    private WeatherSnapshot parse(String json) {
        // current 블록 안에서만 읽기
        String current = extractObject(json, "\"current\"");
        double curTemp = readNumberAfter(current, "\"temperature_2m\":");
        int    curCode = (int) readNumberAfter(current, "\"weather_code\":");

        // daily 블록 안에서만 읽기 (★ 핵심: daily_units 와 분리)
        String daily = extractObject(json, "\"daily\":");
        double[] mins  = readNumberArrayAfter(daily, "\"temperature_2m_min\":");
        double[] maxs  = readNumberArrayAfter(daily, "\"temperature_2m_max\":");
        double[] codes = readNumberArrayAfter(daily, "\"weather_code\":");

        DayForecast today = (mins.length > 0 && maxs.length > 0 && codes.length > 0)
                ? new DayForecast(mins[0], maxs[0], (int) codes[0]) : null;
        DayForecast tomorrow = (mins.length > 1 && maxs.length > 1 && codes.length > 1)
                ? new DayForecast(mins[1], maxs[1], (int) codes[1]) : null;

        return WeatherSnapshot.ok(curTemp, curCode, today, tomorrow);
    }

    /**
     * 주어진 키 다음에 오는 JSON 객체({...}) 의 본문을 추출.
     * 중괄호 깊이를 세서 정확히 매칭된 블록만 반환한다.
     */
    private String extractObject(String json, String key) {
        int kIdx = json.indexOf(key);
        if (kIdx < 0) return "";
        int open = json.indexOf('{', kIdx);
        if (open < 0) return "";
        int depth = 0;
        for (int i = open; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return json.substring(open, i + 1);
            }
        }
        return json.substring(open);
    }

    /** 섹션 내부에서 key 뒤의 숫자 하나 읽기. */
    private double readNumberAfter(String section, String key) {
        if (section == null || section.isEmpty()) return Double.NaN;
        int kIdx = section.indexOf(key);
        if (kIdx < 0) return Double.NaN;
        int start = kIdx + key.length();
        int end = start;
        // 숫자/소수점/마이너스/지수만 허용
        while (end < section.length()) {
            char c = section.charAt(end);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                end++;
            } else if (end == start && Character.isWhitespace(c)) {
                start++; end++;
            } else {
                break;
            }
        }
        try {
            return Double.parseDouble(section.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** 섹션 내부에서 key 뒤의 숫자 배열 [a,b,c] 읽기. */
    private double[] readNumberArrayAfter(String section, String key) {
        if (section == null || section.isEmpty()) return new double[0];
        int kIdx = section.indexOf(key);
        if (kIdx < 0) return new double[0];
        int open  = section.indexOf('[', kIdx);
        int close = section.indexOf(']', open);
        if (open < 0 || close < 0) return new double[0];
        String inner = section.substring(open + 1, close).trim();
        if (inner.isEmpty()) return new double[0];
        String[] parts = inner.split(",");
        double[] arr = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.equals("null")) {
                arr[i] = Double.NaN;
                continue;
            }
            try {
                arr[i] = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                arr[i] = Double.NaN;
            }
        }
        return arr;
    }

    // ============================================================
    //  값 객체
    // ============================================================
    private record CacheEntry(WeatherSnapshot snapshot, long timestamp) {}

    public record DayForecast(double tempMin, double tempMax, int weatherCode) {
        public String tempRangeLabel() {
            return "%.0f° / %.0f°".formatted(tempMin, tempMax);
        }
    }

    public static final class WeatherSnapshot {
        private final boolean success;
        private final String errorMessage;
        private final double currentTemp;
        private final int currentCode;
        private final DayForecast today;
        private final DayForecast tomorrow;

        private WeatherSnapshot(boolean success, String error,
                                double curTemp, int curCode,
                                DayForecast today, DayForecast tomorrow) {
            this.success = success;
            this.errorMessage = error;
            this.currentTemp = curTemp;
            this.currentCode = curCode;
            this.today = today;
            this.tomorrow = tomorrow;
        }

        public static WeatherSnapshot ok(double curTemp, int curCode,
                                         DayForecast today, DayForecast tomorrow) {
            return new WeatherSnapshot(true, null, curTemp, curCode, today, tomorrow);
        }

        public static WeatherSnapshot error(String message) {
            return new WeatherSnapshot(false, message, Double.NaN, -1, null, null);
        }

        public boolean isSuccess()       { return success; }
        public String  getErrorMessage() { return errorMessage; }
        public double  getCurrentTemp()  { return currentTemp; }
        public int     getCurrentCode()  { return currentCode; }
        public DayForecast getToday()    { return today; }
        public DayForecast getTomorrow() { return tomorrow; }
    }

    // ============================================================
    //  WMO weather code → 한국어 라벨 + 아이콘
    // ============================================================
    public static String describe(int code) {
        return switch (code) {
            case 0  -> "맑음";
            case 1  -> "대체로 맑음";
            case 2  -> "구름 조금";
            case 3  -> "흐림";
            case 45, 48 -> "안개";
            case 51, 53, 55 -> "이슬비";
            case 56, 57 -> "어는 이슬비";
            case 61 -> "약한 비";
            case 63 -> "비";
            case 65 -> "강한 비";
            case 66, 67 -> "어는 비";
            case 71 -> "약한 눈";
            case 73 -> "눈";
            case 75 -> "강한 눈";
            case 77 -> "싸락눈";
            case 80 -> "약한 소나기";
            case 81 -> "소나기";
            case 82 -> "강한 소나기";
            case 85, 86 -> "소낙눈";
            case 95 -> "뇌우";
            case 96, 99 -> "우박 동반 뇌우";
            default -> "—";
        };
    }

    public static String icon(int code) {
        return switch (code) {
            case 0 -> "☀";
            case 1, 2 -> "⛅";
            case 3 -> "☁";
            case 45, 48 -> "🌫";
            case 51, 53, 55, 56, 57 -> "🌦";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧";
            case 71, 73, 75, 77, 85, 86 -> "🌨";
            case 95, 96, 99 -> "⛈";
            default -> "·";
        };
    }
}
