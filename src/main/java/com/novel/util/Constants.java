package com.novel.util;

import java.nio.file.Paths;

/**
 * 存储应用程序中使用的常量
 */
public class Constants {
    // 文件路径
    public static final String NOVEL_FILE_PATH = Paths.get(System.getProperty("user.dir"), "牧神记.txt").toString();
    public static final String OUTPUT_DIR = Paths.get(System.getProperty("user.dir"), "output").toString();
    public static final String SPLIT_CHAPTERS_DIR = Paths.get(OUTPUT_DIR, "split_chapters").toString();
    public static final String API_RESULTS_DIR = Paths.get(OUTPUT_DIR, "api_results").toString();
    public static final String FINAL_RESULT_FILE = Paths.get(OUTPUT_DIR, "final_analysis.txt").toString();
    
    // API配置
    public static final String API_KEY_FILE = Paths.get(System.getProperty("user.dir"), "apikey.yml").toString();
    public static final int MAX_REQUESTS_PER_MINUTE = 15;
    public static final int THREAD_COUNT = 10;
    
    // 章节配置
    public static final String CHAPTER_PATTERN = "第[一二三四五六七八九十百千0-9]+章\\s+.*";
    public static final int CHAPTERS_PER_FILE = 10;
    
    // Gemini API配置
    public static final String GEMINI_MODEL = "gemini-1.5-pro-latest";
    public static final int MAX_OUTPUT_TOKENS = 8192;
    public static final double TEMPERATURE = 0.7;
    public static final int MAX_RETRIES = 3;
    public static final long RETRY_DELAY_MS = 5000;
}
