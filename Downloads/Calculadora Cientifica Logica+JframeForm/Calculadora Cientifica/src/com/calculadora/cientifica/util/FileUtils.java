/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Capito
 */

public final class FileUtils {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private FileUtils() {}

    // Crea el directorio si no existe y lo devuelve.
    public static File ensureDir(File dir) throws IOException {
        if (dir == null) throw new IOException("Directorio nulo.");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio: " + dir);
        }
        return dir;
    }

    // Escribe texto en UTF-8 (sobrescribe).
    public static void writeString(File file, String content) throws IOException {
        if (file == null) throw new IOException("Archivo nulo.");
        File parent = file.getParentFile();
        if (parent != null) ensureDir(parent);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            if (content != null) bw.write(content);
        }
    }

    // Escribe texto en UTF-8 (append).
    public static void appendString(File file, String content) throws IOException {
        if (file == null) throw new IOException("Archivo nulo.");
        File parent = file.getParentFile();
        if (parent != null) ensureDir(parent);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            if (content != null) bw.write(content);
        }
    }

    // Escribe varias líneas en UTF-8 (sobrescribe).
    public static void writeLines(File file, List<String> lines) throws IOException {
        if (file == null) throw new IOException("Archivo nulo.");
        File parent = file.getParentFile();
        if (parent != null) ensureDir(parent);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            if (lines != null) {
                for (String line : lines) {
                    bw.write(line == null ? "" : line);
                    bw.newLine();
                }
            }
        }
    }

    // Escribe texto de forma atómica: tmp + move (sobrescribe).
    public static void writeStringAtomic(File file, String content) throws IOException {
        if (file == null) throw new IOException("Archivo nulo.");
        File parent = file.getParentFile();
        if (parent != null) ensureDir(parent);
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        writeString(tmp, content);
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // Escribe líneas de forma atómica: tmp + move (sobrescribe).
    public static void writeLinesAtomic(File file, List<String> lines) throws IOException {
        if (file == null) throw new IOException("Archivo nulo.");
        File parent = file.getParentFile();
        if (parent != null) ensureDir(parent);
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        writeLines(tmp, lines);
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // Lee todo el contenido como String (UTF-8).
    public static String readString(File file) throws IOException {
        if (file == null || !file.exists()) throw new IOException("Archivo no existe: " + file);
        StringBuilder sb = new StringBuilder((int)Math.min(file.length(), 4_194_304)); // hint 4MB
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (!first) sb.append('\n');
                sb.append(line);
                first = false;
            }
        }
        return sb.toString();
    }

    // Lee todas las líneas como lista (UTF-8).
    public static List<String> readLines(File file) throws IOException {
        if (file == null || !file.exists()) throw new IOException("Archivo no existe: " + file);
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) out.add(line);
        }
        return out;
    }

    // Directorio base: {user.home}/CalculadoraCientifica
    public static File getAppDir() {
        return new File(System.getProperty("user.home"), "CalculadoraCientifica");
    }

    // /docs dentro de la app.
    public static File getDocsDir() {
        return new File(getAppDir(), "docs");
    }

    // /exports dentro de la app.
    public static File getExportsDir() {
        return new File(getAppDir(), "exports");
    }

    // Guarda historial en /exports (sobrescribe).
    public static File saveHistory(String fileName, List<String> historyLines) throws IOException {
        if (fileName == null || fileName.isBlank()) fileName = "historial.txt";
        File out = new File(getExportsDir(), sanitizeFileName(fileName));
        writeLines(out, historyLines);
        Logging.info("Historial guardado en: " + out.getAbsolutePath());
        return out;
    }

    // Genera nombre timestamped en /exports.
    public static File timestampedExport(String baseName) {
        if (baseName == null || baseName.isBlank()) baseName = "export";
        String clean = stripExtension(sanitizeFileName(baseName));
        String name = clean + "-" + TS.format(LocalDateTime.now()) + ".txt";
        return new File(getExportsDir(), name);
    }

    // Copia simple (sobrescribe destino).
    public static void copy(File src, File dst) throws IOException {
        if (src == null || dst == null) throw new IOException("Archivo nulo.");
        File parent = dst.getParentFile();
        if (parent != null) ensureDir(parent);
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // Elimina sin lanzar excepción.
    public static boolean deleteQuietly(File f) {
        if (f == null) return false;
        try { return f.delete(); } catch (Exception ignored) { return false; }
    }

    // Sanitiza nombre de archivo (sin separadores ni caracteres inválidos).
    public static String sanitizeFileName(String name) {
        if (name == null) return "file";
        String s = name.trim();
        s = s.replaceAll("[\\\\/:*?\"<>|]+", "_");
        s = s.replaceAll("\\s+", " ");
        return s.isEmpty() ? "file" : s;
    }

    // Quita la extensión.
    public static String stripExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return (dot <= 0) ? name : name.substring(0, dot);
    }
}
