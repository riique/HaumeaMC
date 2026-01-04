package com.haumea.kitpvp.utils;

/**
 * Utilitários para manipulação de tempo.
 * 
 * Centraliza toda a lógica de parsing e formatação de tempo
 * para evitar duplicação em múltiplos managers.
 * 
 * @author HaumeaMC
 */
public class TimeUtils {

    // Constantes de tempo em milissegundos
    public static final long SECOND = 1000L;
    public static final long MINUTE = 60L * SECOND;
    public static final long HOUR = 60L * MINUTE;
    public static final long DAY = 24L * HOUR;
    public static final long WEEK = 7L * DAY;
    public static final long MONTH = 30L * DAY;
    public static final long YEAR = 365L * DAY;

    /**
     * Parseia uma string de tempo para milissegundos.
     * 
     * Formato:
     * - 1s = 1 segundo
     * - 1m = 1 minuto
     * - 1h = 1 hora
     * - 1d = 1 dia
     * - 1w = 1 semana
     * - 1mo = 1 mês (30 dias)
     * - 1y = 1 ano (365 dias)
     * - perm, permanente = permanente (retorna 0)
     * - Combinações: 1d12h30m = 1 dia, 12 horas e 30 minutos
     * 
     * @param timeStr String de tempo (ex: "30d", "1h30m", "1mo")
     * @return duração em milissegundos, 0 se permanente, -1 se inválido
     */
    public static long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return -1;
        }

        String lower = timeStr.toLowerCase().trim();

        // Verificar se é permanente
        if (lower.equals("perm") || lower.equals("permanente") || lower.equals("permanent")) {
            return 0;
        }

        long totalMs = 0;
        StringBuilder number = new StringBuilder();
        char[] chars = lower.toCharArray();
        int i = 0;

        while (i < chars.length) {
            char c = chars[i];

            if (Character.isDigit(c)) {
                number.append(c);
                i++;
            } else {
                if (number.length() == 0) {
                    i++;
                    continue;
                }

                long value = Long.parseLong(number.toString());
                number = new StringBuilder();

                // Verificar sufixo de 2 caracteres (mo)
                if (c == 'm' && i + 1 < chars.length && chars[i + 1] == 'o') {
                    totalMs += value * MONTH;
                    i += 2; // Pular 'mo'
                    continue;
                }

                switch (c) {
                    case 's':
                        totalMs += value * SECOND;
                        break;
                    case 'm':
                        totalMs += value * MINUTE;
                        break;
                    case 'h':
                        totalMs += value * HOUR;
                        break;
                    case 'd':
                        totalMs += value * DAY;
                        break;
                    case 'w':
                        totalMs += value * WEEK;
                        break;
                    case 'y':
                        totalMs += value * YEAR;
                        break;
                    default:
                        return -1; // Sufixo inválido
                }
                i++;
            }
        }

        return totalMs > 0 ? totalMs : -1;
    }

    /**
     * Formata milissegundos para uma string legível.
     * 
     * Exemplo: 90061000 → "1 dia, 1 hora, 1 minuto, 1 segundo"
     * 
     * @param ms Tempo em milissegundos
     * @return String formatada
     */
    public static String formatTime(long ms) {
        if (ms <= 0) {
            return "Permanente";
        }

        long days = ms / DAY;
        ms %= DAY;
        long hours = ms / HOUR;
        ms %= HOUR;
        long minutes = ms / MINUTE;
        ms %= MINUTE;
        long seconds = ms / SECOND;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " dia" : " dias");
        }
        if (hours > 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hora" : " horas");
        }
        if (minutes > 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minuto" : " minutos");
        }
        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " segundo" : " segundos");
        }

        return sb.toString();
    }

    /**
     * Formata milissegundos para uma string curta.
     * 
     * Exemplo: 90061000 → "1d 1h 1m 1s"
     * 
     * @param ms Tempo em milissegundos
     * @return String formatada curta
     */
    public static String formatTimeShort(long ms) {
        if (ms <= 0) {
            return "Permanente";
        }

        long days = ms / DAY;
        ms %= DAY;
        long hours = ms / HOUR;
        ms %= HOUR;
        long minutes = ms / MINUTE;
        ms %= MINUTE;
        long seconds = ms / SECOND;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Formata milissegundos para uma string muito curta (só os 2 maiores
     * componentes).
     * 
     * Exemplo: 90061000 → "1d 1h"
     * 
     * @param ms Tempo em milissegundos
     * @return String formatada curta (máximo 2 componentes)
     */
    public static String formatTimeCompact(long ms) {
        if (ms <= 0) {
            return "Perm";
        }

        long days = ms / DAY;
        ms %= DAY;
        long hours = ms / HOUR;
        ms %= HOUR;
        long minutes = ms / MINUTE;
        ms %= MINUTE;
        long seconds = ms / SECOND;

        StringBuilder sb = new StringBuilder();
        int components = 0;

        if (days > 0 && components < 2) {
            sb.append(days).append("d ");
            components++;
        }
        if (hours > 0 && components < 2) {
            sb.append(hours).append("h ");
            components++;
        }
        if (minutes > 0 && components < 2) {
            sb.append(minutes).append("m ");
            components++;
        }
        if (seconds > 0 && components < 2) {
            sb.append(seconds).append("s");
            components++;
        }

        if (sb.length() == 0) {
            return "0s";
        }

        return sb.toString().trim();
    }

    /**
     * Converte milissegundos para segundos (arredondado para cima).
     * 
     * @param ms Tempo em milissegundos
     * @return Tempo em segundos
     */
    public static int toSeconds(long ms) {
        return (int) Math.ceil(ms / 1000.0);
    }

    /**
     * Converte milissegundos para minutos (arredondado para cima).
     * 
     * @param ms Tempo em milissegundos
     * @return Tempo em minutos
     */
    public static int toMinutes(long ms) {
        return (int) Math.ceil(ms / (60.0 * 1000.0));
    }

    /**
     * Converte dias para milissegundos.
     * 
     * @param days Número de dias
     * @return Tempo em milissegundos
     */
    public static long daysToMs(int days) {
        return days * DAY;
    }

    /**
     * Converte horas para milissegundos.
     * 
     * @param hours Número de horas
     * @return Tempo em milissegundos
     */
    public static long hoursToMs(int hours) {
        return hours * HOUR;
    }

    /**
     * Converte minutos para milissegundos.
     * 
     * @param minutes Número de minutos
     * @return Tempo em milissegundos
     */
    public static long minutesToMs(int minutes) {
        return minutes * MINUTE;
    }

    /**
     * Calcula quanto tempo falta para um timestamp.
     * 
     * @param expiration Timestamp de expiração
     * @return Tempo restante em ms (0 se já expirou)
     */
    public static long getTimeRemaining(long expiration) {
        if (expiration == 0) {
            return 0; // Permanente
        }
        long remaining = expiration - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Verifica se um timestamp já expirou.
     * 
     * @param expiration Timestamp de expiração (0 = nunca expira)
     * @return true se expirou
     */
    public static boolean isExpired(long expiration) {
        if (expiration == 0) {
            return false; // Permanente nunca expira
        }
        return System.currentTimeMillis() >= expiration;
    }
}
