package com.novel;

import com.novel.config.ApiKeyConfig;
import com.novel.processor.ChapterProcessor;
import com.novel.processor.GeminiApiProcessor;
import com.novel.service.ChapterFileService;
import com.novel.service.RateLimiterService;
import com.novel.service.impl.ChapterFileServiceImpl;
import com.novel.service.impl.GeminiApiServiceImpl;
import com.novel.service.impl.RateLimiterServiceImpl;
import com.novel.util.Constants;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 主应用程序类，用于协调小说处理和Gemini API调用的整个流程
 */
public class NovelReaderApp {
    private static final Logger logger = LoggerFactory.getLogger(NovelReaderApp.class);

    public static void main(String[] args) {
        logger.info("开始处理小说文件");
        
        try {
            // 1. 加载API密钥
            ApiKeyConfig apiKeyConfig = new ApiKeyConfig();
            apiKeyConfig.loadApiKey();
            
            // 2. 获取小说文件路径
            Path novelFilePath = Paths.get(Constants.NOVEL_FILE_PATH);
            File novelFile = novelFilePath.toFile();
            
            if (!novelFile.exists()) {
                logger.error("小说文件不存在: {}", Constants.NOVEL_FILE_PATH);
                System.exit(1);
            }
            
            // 3. 处理章节
            ChapterProcessor chapterProcessor = new ChapterProcessor();
            boolean chapterValidationResult = chapterProcessor.validateChapters(novelFile);
            
            if (!chapterValidationResult) {
                logger.error("章节验证失败，程序终止");
                System.exit(1);
            }
            
            // 4. 分割小说
            boolean splitResult = chapterProcessor.splitNovelByChapters(novelFile);
            
            if (!splitResult) {
                logger.error("小说分割失败，程序终止");
                System.exit(1);
            }
            
            // 5. 调用Gemini API
            GeminiApiProcessor apiProcessor = new GeminiApiProcessor(
                new GeminiApiServiceImpl(apiKeyConfig.getApiKey()),
                new ChapterFileServiceImpl(),
                new RateLimiterServiceImpl(Executors.newScheduledThreadPool(10)),
                Executors.newFixedThreadPool(10)
            );
            boolean apiCallResult = apiProcessor.processChapterFiles();
            
            if (!apiCallResult) {
                logger.error("Gemini API调用失败，程序终止");
                System.exit(1);
            }
            
            logger.info("小说处理完成");
            
        } catch (Exception e) {
            logger.error("处理过程中发生错误", e);
            System.exit(1);
        }
    }
}
