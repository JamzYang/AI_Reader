package com.novel.processor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.novel.util.Constants;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 处理Gemini API的调用和结果处理
 */
public class GeminiApiProcessor {
    private static final Logger logger = LoggerFactory.getLogger(GeminiApiProcessor.class);
    private final String apiKey;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Pattern fileNamePattern = Pattern.compile("(\\d+)第(\\d+)-(\\d+)章\\.txt");
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final ScheduledExecutorService rateLimiter = Executors.newScheduledThreadPool(1);

    public GeminiApiProcessor(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 处理所有分割后的章节文件
     */
    public boolean processChapterFiles() {
        logger.info("开始处理章节文件...");

        try {
            // 创建输出目录
            Path apiResultsDir = Paths.get(Constants.API_RESULTS_DIR);
            if (!Files.exists(apiResultsDir)) {
                Files.createDirectories(apiResultsDir);
            }

            // 获取所有分割后的章节文件
            File splitChaptersDir = new File(Constants.SPLIT_CHAPTERS_DIR);
            if (!splitChaptersDir.exists() || !splitChaptersDir.isDirectory()) {
                logger.error("分割章节目录不存在: {}", Constants.SPLIT_CHAPTERS_DIR);
                return false;
            }

            File[] chapterFiles = splitChaptersDir.listFiles((dir, name) -> name.matches("\\d+第\\d+-\\d+章\\.txt"));
            if (chapterFiles == null || chapterFiles.length == 0) {
                logger.error("没有找到分割后的章节文件");
                return false;
            }

            // 按文件名排序
            Arrays.sort(chapterFiles, (f1, f2) -> {
                Matcher m1 = fileNamePattern.matcher(f1.getName());
                Matcher m2 = fileNamePattern.matcher(f2.getName());
                
                if (m1.find() && m2.find()) {
                    int index1 = Integer.parseInt(m1.group(1));
                    int index2 = Integer.parseInt(m2.group(1));
                    return Integer.compare(index1, index2);
                }
                return f1.getName().compareTo(f2.getName());
            });

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
            List<Future<ApiResult>> futures = new ArrayList<>();

            // 提交任务
            for (File chapterFile : chapterFiles) {
                futures.add(executor.submit(() -> processChapterFile(chapterFile)));
            }

            // 收集结果
            List<ApiResult> results = new ArrayList<>();
            for (Future<ApiResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("获取API调用结果时出错", e);
                }
            }

            // 关闭线程池
            executor.shutdown();
            rateLimiter.shutdown();

            // 合并结果
            mergeResults(results);

            logger.info("所有章节文件处理完成");
            return true;

        } catch (Exception e) {
            logger.error("处理章节文件时出错", e);
            return false;
        }
    }

    /**
     * 处理单个章节文件
     */
    private ApiResult processChapterFile(File chapterFile) {
        logger.info("处理章节文件: {}", chapterFile.getName());

        try {
            // 读取章节内容
            String content = Files.readString(chapterFile.toPath(), StandardCharsets.UTF_8);

            // 提取文件名中的章节范围
            Matcher matcher = fileNamePattern.matcher(chapterFile.getName());
            int fileIndex = 0;
            int startChapter = 0;
            int endChapter = 0;

            if (matcher.find()) {
                fileIndex = Integer.parseInt(matcher.group(1));
                startChapter = Integer.parseInt(matcher.group(2));
                endChapter = Integer.parseInt(matcher.group(3));
            }

            // 构建提示词
            String prompt = loadPrompt();
            prompt = prompt + "\n\n以下是《牧神记》第" + startChapter + "章到第" + endChapter + "章的内容：\n\n" + content;

            // 限流控制
            rateLimitControl();

            // 调用Gemini API
            String apiResponse = callGeminiApi(prompt);

            // 保存结果
            String outputFileName = String.format("%03d第%d-%d章_分析.json", fileIndex, startChapter, endChapter);
            Path outputFilePath = Paths.get(Constants.API_RESULTS_DIR, outputFileName);

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("file_index", fileIndex);
            resultJson.addProperty("start_chapter", startChapter);
            resultJson.addProperty("end_chapter", endChapter);
            resultJson.addProperty("analysis", apiResponse);

            Files.write(outputFilePath, gson.toJson(resultJson).getBytes(StandardCharsets.UTF_8));
            logger.info("已保存API结果: {}", outputFileName);

            return new ApiResult(fileIndex, startChapter, endChapter, apiResponse);

        } catch (Exception e) {
            logger.error("处理章节文件时出错: {}", chapterFile.getName(), e);
            return new ApiResult(-1, -1, -1, "处理出错: " + e.getMessage());
        }
    }

    /**
     * 调用Gemini API
     */
    private String callGeminiApi(String prompt) {
        try {
            // 构建请求体
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            // 构建请求
            HttpClient client = HttpClient.newHttpClient();
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    Constants.GEMINI_MODEL, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 处理响应
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                logger.error("API调用失败，状态码: {}", response.statusCode());
                throw new IOException("API调用失败，状态码: " + response.statusCode() + ", 响应体: " + response.body());
            }

        } catch (Exception e) {
            // 重试逻辑
            for (int retry = 0; retry < Constants.MAX_RETRIES; retry++) {
                logger.warn("API调用失败，尝试重试 {}/{}", retry + 1, Constants.MAX_RETRIES);
                try {
                    Thread.sleep(Constants.RETRY_DELAY_MS);
                    
                    // 构建请求体
                    JsonObject requestBody = new JsonObject();
                    JsonArray contents = new JsonArray();
                    JsonObject content = new JsonObject();
                    JsonArray parts = new JsonArray();
                    JsonObject part = new JsonObject();
                    part.addProperty("text", prompt);
                    parts.add(part);
                    content.add("parts", parts);
                    contents.add(content);
                    requestBody.add("contents", contents);

                    // 构建请求
                    HttpClient client = HttpClient.newHttpClient();
                    String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                            Constants.GEMINI_MODEL, apiKey);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                            .build();

                    // 发送请求
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // 处理响应
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        logger.error("API调用失败，状态码: {}", response.statusCode());
                        throw new IOException("API调用失败，状态码: " + response.statusCode() + ", 响应体: " + response.body());
                    }

                } catch (Exception retryEx) {
                    logger.error("重试失败", retryEx);
                }
            }
            throw new RuntimeException("API调用失败，已达到最大重试次数", e);
        }
    }

    /**
     * 限流控制
     */
    private void rateLimitControl() throws InterruptedException {
        int currentCount = requestCounter.incrementAndGet();
        
        // 如果已经达到每分钟的请求限制，等待下一个时间窗口
        if (currentCount > Constants.MAX_REQUESTS_PER_MINUTE) {
            logger.info("已达到每分钟请求限制，等待下一个时间窗口...");
            
            // 等待直到下一分钟
            CountDownLatch latch = new CountDownLatch(1);
            rateLimiter.schedule(() -> {
                requestCounter.set(1); // 重置计数器
                latch.countDown();
            }, 60, TimeUnit.SECONDS);
            
            latch.await();
        }
    }

    /**
     * 加载提示词
     */
    private String loadPrompt() {
        try {
            Path promptPath = Paths.get(System.getProperty("user.dir"), "prompt.txt");
            if (Files.exists(promptPath)) {
                return Files.readString(promptPath, StandardCharsets.UTF_8);
            } else {
                // 如果文件不存在，使用需求文档中的提示词
                return "请你阅读并逐步分析《牧神记》的每一章节。在分析中，请重点关注以下几个方面，并尽可能提供详细的描写和具体的情节：\n\n" +
                        "男主角的经历：\n\n" +
                        "男主角在本章节中的冒险旅程、日常生活，以及遇到的具体事件。\n\n" +
                        "描述男主角在这些章节中的成长，体现在性格、技能、心境等方面的变化。\n\n" +
                        "男主角在这些章节中是否有重要的转折点或关键事件？如果有，请详细描述事件的内容和影响。\n\n" +
                        "世界观与设定：\n\n" +
                        "在本章节中，小说世界观有什么体现？\n\n" +
                        "详细解释本章节涉及的地理环境、设定、特殊规则和力量原理\n\n" +
                        "人物关系：\n\n" +
                        "列出并详细描述与男主角相关的关键人物在本章节中的表现，他们之间的互动、对话，以及对主角的影响。\n" +
                        "他们的背景、性格，以及与主角的关系。\n\n" +
                        "虚构历史：\n\n" +
                        "如果本章节提到重要的历史事件、传说、神话，请详细说明。\n" +
                        "这些历史事件和传说对当前世界和男主角产生了什么影响？\n" +
                        "其他重要细节：\n\n" +
                        "除了上述四点，请关注章节中任何重要的细节，包括伏笔、线索、暗示等。\n\n" +
                        "总结每章的关键内容，并分析章节之间的关联性。\n\n" +
                        "请注意：\n\n" +
                        "避免过于简略，重点突出情节的细节，尽可能用具体的例子和描述来支撑你的分析。\n\n" +
                        "对于战斗场景，可以简要概括，但重点要放在战斗的结果、对剧情的影响，以及战斗中体现的人物关系和设定。\n\n" +
                        "对伏笔、线索等进行提示，并分析其可能的影响。";
            }
        } catch (IOException e) {
            logger.error("加载提示词时出错", e);
            // 返回默认提示词
            return "请分析这几章小说的内容，包括人物、情节、主题等方面。";
        }
    }

    /**
     * 合并所有API调用结果
     */
    private void mergeResults(List<ApiResult> results) {
        logger.info("开始合并API调用结果...");

        try {
            // 按文件索引排序
            results.sort(Comparator.comparingInt(ApiResult::getFileIndex));

            // 创建输出目录
            if (!Files.exists(Paths.get(Constants.OUTPUT_DIR))) {
                Files.createDirectories(Paths.get(Constants.OUTPUT_DIR));
            }

            // 合并结果
            StringBuilder mergedContent = new StringBuilder();
            mergedContent.append("# 《牧神记》小说分析\n\n");

            for (ApiResult result : results) {
                if (result.getFileIndex() > 0) { // 跳过错误结果
                    mergedContent.append("## 第").append(result.getStartChapter())
                            .append("章 - 第").append(result.getEndChapter()).append("章分析\n\n");
                    mergedContent.append(result.getAnalysis()).append("\n\n");
                    mergedContent.append("---\n\n");
                }
            }

            // 写入最终结果文件
            Files.write(Paths.get(Constants.FINAL_RESULT_FILE), 
                    mergedContent.toString().getBytes(StandardCharsets.UTF_8));
            logger.info("结果合并完成，已保存到: {}", Constants.FINAL_RESULT_FILE);

        } catch (IOException e) {
            logger.error("合并结果时出错", e);
        }
    }

    /**
     * API调用结果类
     */
    private static class ApiResult {
        private final int fileIndex;
        private final int startChapter;
        private final int endChapter;
        private final String analysis;

        public ApiResult(int fileIndex, int startChapter, int endChapter, String analysis) {
            this.fileIndex = fileIndex;
            this.startChapter = startChapter;
            this.endChapter = endChapter;
            this.analysis = analysis;
        }

        public int getFileIndex() {
            return fileIndex;
        }

        public int getStartChapter() {
            return startChapter;
        }

        public int getEndChapter() {
            return endChapter;
        }

        public String getAnalysis() {
            return analysis;
        }
    }
}
