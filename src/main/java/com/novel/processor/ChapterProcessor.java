package com.novel.processor;

import com.novel.model.Chapter;
import com.novel.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 处理小说章节的验证、分割和合并
 */
public class ChapterProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ChapterProcessor.class);
    private final List<Chapter> chapters = new ArrayList<>();
    private final Pattern chapterPattern = Pattern.compile(Constants.CHAPTER_PATTERN);

    /**
     * 验证小说章节是否按顺序递增，是否有重复或缺失
     */
    public boolean validateChapters(File novelFile) {
        logger.info("开始验证章节...");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(novelFile), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            Set<Integer> chapterNumbers = new HashSet<>();
            List<Integer> missingChapters = new ArrayList<>();
            List<Integer> duplicateChapters = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                Matcher matcher = chapterPattern.matcher(line);
                
                if (matcher.matches()) {
                    String chapterTitle = line.trim();
                    int chapterNumber = extractChapterNumber(chapterTitle);
                    
                    if (chapterNumber > 0) {
                        if (chapterNumbers.contains(chapterNumber)) {
                            logger.warn("发现重复章节: 第{}章，行号: {}", chapterNumber, lineNumber);
                            duplicateChapters.add(chapterNumber);
                        } else {
                            chapterNumbers.add(chapterNumber);
                            chapters.add(new Chapter(chapterNumber, chapterTitle, lineNumber));
                        }
                    } else {
                        logger.warn("无法提取章节编号: {}, 行号: {}", chapterTitle, lineNumber);
                    }
                }
            }
            
            // 检查章节是否连续
            int maxChapter = Collections.max(chapterNumbers);
            for (int i = 1; i <= maxChapter; i++) {
                if (!chapterNumbers.contains(i)) {
                    logger.warn("缺失章节: 第{}章", i);
                    missingChapters.add(i);
                }
            }
            
            // 排序章节
            chapters.sort(Comparator.comparingInt(Chapter::getNumber));
            
            // 最后一章识别完成后，如果发现重复或缺失的章节，打印日志并中断程序
            if (!missingChapters.isEmpty() || !duplicateChapters.isEmpty()) {
                if (!missingChapters.isEmpty()) {
                    logger.error("缺失章节列表: {}", missingChapters);
                }
                if (!duplicateChapters.isEmpty()) {
                    logger.error("重复章节列表: {}", duplicateChapters);
                }
                return false;
            }
            
            logger.info("章节验证通过，共发现{}个章节", chapters.size());
            return true;
            
        } catch (IOException e) {
            logger.error("验证章节时出错", e);
            return false;
        }
    }
    
    /**
     * 从章节标题中提取章节编号
     */
    private int extractChapterNumber(String chapterTitle) {
        // 处理中文数字
        if (chapterTitle.contains("第") && chapterTitle.contains("章")) {
            String numPart = chapterTitle.substring(chapterTitle.indexOf("第") + 1, chapterTitle.indexOf("章"));
            
            // 尝试直接解析阿拉伯数字
            try {
                return Integer.parseInt(numPart.trim());
            } catch (NumberFormatException e) {
                // 如果不是阿拉伯数字，尝试转换中文数字
                return convertChineseNumberToArabic(numPart.trim());
            }
        }
        return -1;
    }
    
    /**
     * 将中文数字转换为阿拉伯数字
     */
    private int convertChineseNumberToArabic(String chineseNumber) {
        Map<Character, Integer> chineseNumMap = new HashMap<>();
        chineseNumMap.put('一', 1);
        chineseNumMap.put('二', 2);
        chineseNumMap.put('三', 3);
        chineseNumMap.put('四', 4);
        chineseNumMap.put('五', 5);
        chineseNumMap.put('六', 6);
        chineseNumMap.put('七', 7);
        chineseNumMap.put('八', 8);
        chineseNumMap.put('九', 9);
        chineseNumMap.put('十', 10);
        chineseNumMap.put('百', 100);
        chineseNumMap.put('千', 1000);
        
        // 处理特殊情况
        if (chineseNumber.equals("十")) {
            return 10;
        }
        
        int result = 0;
        int temp = 0;
        int multiple = 1;
        
        for (int i = 0; i < chineseNumber.length(); i++) {
            char c = chineseNumber.charAt(i);
            if (chineseNumMap.containsKey(c)) {
                int num = chineseNumMap.get(c);
                if (num >= 10) {
                    if (temp == 0) {
                        temp = 1;
                    }
                    result += temp * num;
                    temp = 0;
                    multiple = num / 10;
                } else {
                    temp = num;
                    if (i == chineseNumber.length() - 1) {
                        result += temp;
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 按章节分割小说并每10章合并为一个文件
     */
    public boolean splitNovelByChapters(File novelFile) {
        logger.info("开始按章节分割小说...");
        
        if (chapters.isEmpty()) {
            logger.error("没有章节信息，请先验证章节");
            return false;
        }
        
        try {
            // 创建输出目录
            Path splitChaptersDir = Paths.get(Constants.SPLIT_CHAPTERS_DIR);
            if (!Files.exists(splitChaptersDir)) {
                Files.createDirectories(splitChaptersDir);
            }
            
            // 读取整个小说文件
            List<String> allLines = Files.readAllLines(novelFile.toPath(), StandardCharsets.UTF_8);
            
            // 按章节分组
            int totalChapters = chapters.size();
            int fileCount = (int) Math.ceil((double) totalChapters / Constants.CHAPTERS_PER_FILE);
            
            for (int fileIndex = 0; fileIndex < fileCount; fileIndex++) {
                int startChapterIndex = fileIndex * Constants.CHAPTERS_PER_FILE;
                int endChapterIndex = Math.min((fileIndex + 1) * Constants.CHAPTERS_PER_FILE - 1, totalChapters - 1);
                
                int startChapterNumber = chapters.get(startChapterIndex).getNumber();
                int endChapterNumber = chapters.get(endChapterIndex).getNumber();
                
                String outputFileName = String.format("%03d第%d-%d章.txt", 
                        fileIndex + 1, startChapterNumber, endChapterNumber);
                Path outputFilePath = Paths.get(Constants.SPLIT_CHAPTERS_DIR, outputFileName);
                
                // 获取章节内容
                StringBuilder content = new StringBuilder();
                
                for (int i = startChapterIndex; i <= endChapterIndex; i++) {
                    Chapter currentChapter = chapters.get(i);
                    int startLine = currentChapter.getLineNumber() - 1; // 0-based index
                    
                    int endLine;
                    if (i < totalChapters - 1) {
                        endLine = chapters.get(i + 1).getLineNumber() - 2; // 前一行
                    } else {
                        endLine = allLines.size() - 1; // 文件末尾
                    }
                    
                    // 添加章节内容
                    content.append(currentChapter.getTitle()).append("\n\n");
                    for (int lineIdx = startLine + 1; lineIdx <= endLine; lineIdx++) {
                        if (lineIdx < allLines.size()) {
                            content.append(allLines.get(lineIdx)).append("\n");
                        }
                    }
                    content.append("\n\n");
                }
                
                // 写入文件
                Files.write(outputFilePath, content.toString().getBytes(StandardCharsets.UTF_8));
                logger.info("已创建分割文件: {}", outputFileName);
            }
            
            logger.info("小说分割完成，共创建{}个文件", fileCount);
            return true;
            
        } catch (IOException e) {
            logger.error("分割小说时出错", e);
            return false;
        }
    }
    
    /**
     * 获取所有章节信息
     */
    public List<Chapter> getChapters() {
        return new ArrayList<>(chapters);
    }
}
