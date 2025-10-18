/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 
 * @author Capito
 */

public final class Logging {

    public enum Level { ERROR, WARN, INFO, DEBUG }

    private static final Object LOCK = new Object();

    // Formateador de fecha thread-safe
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    // Configuración
    private static volatile long maxBytes = 5L * 1024L * 1024L; // 5 MB
    private static volatile Level currentLevel = Level.INFO;
    private static volatile boolean consoleEnabled = true;

    private static BufferedWriter fileWriter = null;
    private static File logFile = null;
    private static File logDir = null;
    private static String baseFileName = "app.log";

    private Logging() {}

    // Nivel global
    public static void setLevel(Level level) {
        if (level != null) currentLevel = level;
    }

    // Alterna salida a consola
    public static void setConsoleEnabled(boolean enabled) {
        consoleEnabled = enabled;
    }

    // Tamaño máximo para rotación
    public static void setMaxFileSizeBytes(long bytes) {
        if (bytes > 0) maxBytes = bytes;
    }

    // Directorio de salida (se aplica al habilitar file logging)
    public static void setOutputDirectory(File dir) {
        synchronized (LOCK) {
            logDir = dir;
        }
    }

    // Nombre base de archivo (se aplica al habilitar file logging)
    public static void setBaseFileName(String name) {
        if (name == null || name.isEmpty()) return;
        synchronized (LOCK) {
            baseFileName = name;
        }
    }

    // Habilita escritura en archivo
    public static void enableFileLogging() {
        synchronized (LOCK) {
            if (fileWriter != null) return;
            try {
                if (logDir == null) {
                    logDir = new File(System.getProperty("user.home"), "CalculadoraCientifica/logs");
                }
                if (!logDir.exists() && !logDir.mkdirs()) {
                    System.err.println("[Logging] No se pudo crear el directorio de logs: " + logDir);
                    return;
                }
                logFile = new File(logDir, baseFileName);
                openWriter(false);
                addShutdownHook();
                info("File logging habilitado en: " + logFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[Logging] No se pudo habilitar file logging: " + e.getMessage());
                fileWriter = null;
            }
        }
    }

    // Cierra el archivo de log si está habilitado
    public static void close() {
        synchronized (LOCK) {
            if (fileWriter != null) {
                try { fileWriter.flush(); fileWriter.close(); }
                catch (IOException ignored) { }
                finally { fileWriter = null; }
            }
        }
    }

    // Fuerza flush del escritor actual
    public static void flush() {
        synchronized (LOCK) {
            if (fileWriter != null) {
                try { fileWriter.flush(); } catch (IOException ignored) { }
            }
        }
    }

    // API pública
    public static void error(String msg) { log(Level.ERROR, msg, null); }
    public static void error(String msg, Throwable t) { log(Level.ERROR, msg, t); }
    public static void warn(String msg) { log(Level.WARN, msg, null); }
    public static void info(String msg) { log(Level.INFO, msg, null); }
    public static void debug(String msg) { log(Level.DEBUG, msg, null); }

    // Overloads con Supplier (evalúa solo si corresponde)
    public static void debug(java.util.function.Supplier<String> supplier) {
        if (shouldLog(Level.DEBUG)) log(Level.DEBUG, safeGet(supplier), null);
    }
    public static void info(java.util.function.Supplier<String> supplier) {
        if (shouldLog(Level.INFO)) log(Level.INFO, safeGet(supplier), null);
    }

    // Internos

    private static void log(Level level, String msg, Throwable t) {
        if (!shouldLog(level)) return;

        final String line = formatLine(level, (msg == null ? "" : msg), t);
        final String stack = (t != null) ? stackTraceToString(t) : null;

        if (consoleEnabled) {
            if (level == Level.ERROR) System.err.println(line);
            else System.out.println(line);
            if (stack != null) {
                if (level == Level.ERROR) System.err.print(stack);
                else System.out.print(stack);
            }
        }

        synchronized (LOCK) {
            if (fileWriter != null) {
                try {
                    rotateIfNeeded();
                    fileWriter.write(line);
                    fileWriter.newLine();
                    if (stack != null) fileWriter.write(stack);
                    fileWriter.flush();
                } catch (IOException e) {
                    System.err.println("[Logging] Error escribiendo log: " + e.getMessage());
                }
            }
        }
    }

    private static boolean shouldLog(Level level) {
        return level.ordinal() <= currentLevel.ordinal();
    }

    private static String formatLine(Level level, String msg, Throwable t) {
        String ts = TS.format(Instant.now());
        String thread = Thread.currentThread().getName();
        String base = String.format("%s [%s] %-5s - %s", ts, thread, level, msg);
        if (t != null) {
            base += " | " + t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
        }
        return base;
    }

    private static String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter(512);
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private static void rotateIfNeeded() throws IOException {
        if (logFile == null) return;
        if (!logFile.exists()) return;
        if (logFile.length() < maxBytes) return;

        close();

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        File rotated = new File(logDir, baseNameWithoutExt(baseFileName) + "-" + stamp + ".log");
        boolean renamed = logFile.renameTo(rotated);
        if (!renamed) {
            // fallback: crea un nuevo archivo base si el rename falla
            rotated = null;
        }

        logFile = new File(logDir, baseFileName);
        openWriter(false);

        if (rotated != null) info("Log rotado: " + rotated.getAbsolutePath());
        else info("Rotación: rename falló, se reinició el archivo base.");
    }

    private static String baseNameWithoutExt(String name) {
        int dot = name.lastIndexOf('.');
        return (dot <= 0) ? name : name.substring(0, dot);
    }

    private static void openWriter(boolean append) throws IOException {
        fileWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(logFile, append), StandardCharsets.UTF_8));
    }

    private static void addShutdownHook() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Logging::close, "logging-shutdown"));
        } catch (IllegalStateException ignored) { }
    }

    // Archivo de log actual
    public static File getLogFile() {
        synchronized (LOCK) {
            return logFile;
        }
    }

    private static String safeGet(java.util.function.Supplier<String> supplier) {
        try { return supplier.get(); } catch (Throwable t) { return "[supplier-failed]"; }
    }
}