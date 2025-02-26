package com.novel.processor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.novel.model.ApiRequest;
import com.novel.model.ApiResponse;
import com.novel.model.Chapter;
import com.novel.service.ChapterFileService;
import com.novel.service.GeminiApiService;
import com.novel.service.RateLimiterService;
import com.novel.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 处理Gemini API的调用和结果处理
 */
public class GeminiApiProcessor {
    private static final Logger logger = LoggerFactory.getLogger(GeminiApiProcessor.class);
    private final GeminiApiService apiService;
    private final ChapterFileService fileService;
    private final RateLimiterService rateLimiter;
    private final Gson gson;
    private final ExecutorService executor;

    public GeminiApiProcessor(
            GeminiApiService apiService,
            ChapterFileService fileService,
            RateLimiterService rateLimiter,
            ExecutorService executor) {
        this.apiService = apiService;
        this.fileService = fileService;
        this.rateLimiter = rateLimiter;
        this.executor = executor;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * 处理所有分割后的章节文件
     */
    public boolean processChapterFiles() {
        logger.info("开始处理章节文件...");

        try {
            // 获取所有章节文件
            List<File> chapterFiles = fileService.getAllChapterFiles();
            List<Future<String>> futures = new ArrayList<>();

            // 提交任务
            for (File chapterFile : chapterFiles) {
                futures.add(executor.submit(() -> processChapterFile(chapterFile)));
            }

            // 收集结果
            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    logger.error("获取API调用结果时出错", e);
                }
            }

            // 合并结果
            mergeResults(results);

            logger.info("所有章节文件处理完成");
            return true;

        } catch (Exception e) {
            logger.error("处理章节文件时出错", e);
            return false;
        } finally {
            executor.shutdown();
            rateLimiter.shutdown();
        }
    }

    /**
     * 处理单个章节文件
     */
    private String processChapterFile(File chapterFile) {
        logger.info("处理章节文件: {}", chapterFile.getName());

        try {
            // 读取章节内容
            String content = fileService.readChapterContent(chapterFile);

            // 解析章节信息
            Chapter chapter = fileService.parseChapterInfo(chapterFile);

            // 构建提示词
            String prompt = loadPrompt();
            prompt = prompt + "\n\n以下是《牧神记》第" + chapter.getStartChapter() + 
                    "章到第" + chapter.getEndChapter() + "章的内容：\n\n" + content;

            // 限流控制
            rateLimiter.acquire();

            // 调用API
            ApiRequest request = new ApiRequest(prompt, Constants.MAX_RETRIES, Constants.RETRY_DELAY_MS);
            ApiResponse response = apiService.callApi(request);

            if (!response.isSuccess()) {
                throw new RuntimeException("API调用失败: " + response.getError());
            }

            // 保存结果
            String outputFileName = String.format("%03d第%d-%d章_分析.json", 
                    chapter.getFileIndex(), chapter.getStartChapter(), chapter.getEndChapter());

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("file_index", chapter.getFileIndex());
            resultJson.addProperty("start_chapter", chapter.getStartChapter());
            resultJson.addProperty("end_chapter", chapter.getEndChapter());
            resultJson.addProperty("analysis", response.getContent());

            fileService.saveAnalysisResult(outputFileName, gson.toJson(resultJson));
            return response.getContent();

        } catch (Exception e) {
            logger.error("处理章节文件时出错: {}", chapterFile.getName(), e);
            return null;
        }
    }

    /**
     * 加载提示词
     */
    private String loadPrompt() throws Exception {
        Path promptPath = Paths.get(System.getProperty("user.dir"), "prompt.txt");
        if (Files.exists(promptPath)) {
            return Files.readString(promptPath);
        }
        return getDefaultPrompt();
    }

    /**
     * 获取默认提示词
     */
    private String getDefaultPrompt() {
        return "请你阅读并逐步分析《牧神记》的每一章节。在分析中，请重点关注以下几个方面，并尽可能提供详细的描写和具体的情节：\n\n" +
                "男主角的经历：\n\n" +
                "男主角在本章节中的冒险旅程、日常生活，以及遇到的具体事件。\n\n" +
                "描述男主角在这些章节中的成长，体现在性格、技能、心境等方面的变化。\n\n" +
                "男主角在这些章节中是否有重要的转折点或关键事件？如果有，请详细描述事件的内容和影响。\n\n" +
                "世界观与设定：\n\n" +
                "在本章节中，小说世界观有什么体现？\n\n" +
                "详细解释本章节涉及的地理环境、设定、特殊规则和力量原理\n\n" +
                "人物关系：\n\n" +
                "列出并详细描述与男主角互动的重要角色，包括他们的性格特点和与主角的关系变化。";
    }

    /**
     * 合并所有API调用结果
     */
    private void mergeResults(List<String> results) throws Exception {
        StringBuilder finalResult = new StringBuilder();
        finalResult.append("《牧神记》小说分析报告\n\n");
        
        for (String result : results) {
            finalResult.append(result).append("\n\n");
        }
        
        Files.writeString(Paths.get(Constants.FINAL_RESULT_FILE), finalResult.toString());
        logger.info("已生成最终分析报告: {}", Constants.FINAL_RESULT_FILE);
    }
}
