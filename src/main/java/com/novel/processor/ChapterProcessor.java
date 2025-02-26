package com.novel.processor;

import com.novel.model.Chapter;
import com.novel.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class ChapterProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ChapterProcessor.class);
    private final List<Chapter> chapters = new ArrayList<>();
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("第\\d+章");

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
                if (lineNumber % 1000 == 0) {
                    logger.debug("正在处理第{}行", lineNumber);
                }
                
                if (isChapterTitle(line)) {
                    int chapterNumber = extractChapterNumber(line);
                    if (chapterNumber > 0 && chapterNumber <= Constants.MAX_CHAPTER_NUMBER) {
                        if (chapterNumbers.contains(chapterNumber)) {
                            logger.warn("发现重复章节: 第{}章，行号: {}", chapterNumber, lineNumber);
                            duplicateChapters.add(chapterNumber);
                        } else {
                            chapterNumbers.add(chapterNumber);
                            chapters.add(new Chapter(chapterNumber, line.trim(), lineNumber));
                            logger.info("成功添加章节: {}", line.trim());
                        }
                    }
                }
            }
            
            // 检查章节是否连续
            if (!chapterNumbers.isEmpty()) {
                int maxChapter = Collections.max(chapterNumbers);
                for (int i = 1; i <= maxChapter; i++) {
                    if (!chapterNumbers.contains(i)) {
                        logger.warn("缺失章节: 第{}章", i);
                        missingChapters.add(i);
                    }
                }
            }
            
            // 排序章节
            chapters.sort(Comparator.comparingInt(Chapter::getNumber));
            
            // 检查是否有问题
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
     * 判断是否是章节标题
     */
    private static boolean isChapterTitle(String line) {
        return line != null && CHAPTER_PATTERN.matcher(line).find();
    }

    /**
     * 提取章节号
     */
    private static int extractChapterNumber(String line) {
        try {
            String numStr = line.substring(line.indexOf("第") + 1, line.indexOf("章"));
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 按章节分割小说并每10章合并为一个文件
     */
    public boolean splitNovelByChapters(File novelFile) {
        if (chapters.isEmpty()) {
            logger.error("没有找到有效的章节，无法分割小说");
            return false;
        }

        try {
            File outputDir = new File("output");
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                logger.error("创建输出目录失败");
                return false;
            }

            // 读取所有内容
            List<String> allLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(novelFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
            }

            // 按每10章分组处理
            int totalChapters = chapters.size();
            int fileIndex = 1;
            
            for (int i = 0; i < totalChapters; i += Constants.CHAPTERS_PER_FILE) {
                int endIndex = Math.min(i + Constants.CHAPTERS_PER_FILE, totalChapters);
                
                // 获取当前文件的内容范围
                int startLine = chapters.get(i).getLineNumber() - 1;  // 0-based index
                int endLine;
                
                if (endIndex < totalChapters) {
                    // 如果不是最后一组，使用下一章的起始行作为结束位置
                    endLine = chapters.get(endIndex).getLineNumber() - 1;
                } else {
                    // 如果是最后一组，使用文件总行数作为结束位置
                    endLine = allLines.size();
                }
                
                // 准备当前文件内容
                List<String> content = new ArrayList<>(allLines.subList(startLine, endLine));
                
                // 计算文件名中的章节范围
                int startChapter = ((i / Constants.CHAPTERS_PER_FILE) * Constants.CHAPTERS_PER_FILE) + 1;
                int endChapter = Math.min(startChapter + Constants.CHAPTERS_PER_FILE - 1, Constants.MAX_CHAPTER_NUMBER);
                
                // 创建文件名
                String fileName = String.format("%03d第%d-%d章.txt",
                        fileIndex++,
                        startChapter,
                        endChapter);
                
                // 写入文件
                writeToFile(new File(outputDir, fileName), content);
                logger.info("已创建文件: {} (实际包含第{}章到第{}章)", 
                    fileName, 
                    chapters.get(i).getNumber(), 
                    chapters.get(endIndex - 1).getNumber());
            }

            return true;
        } catch (IOException e) {
            logger.error("分割小说时出错", e);
            return false;
        }
    }

    private void writeToFile(File file, List<String> content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (String line : content) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    public List<Chapter> getChapters() {
        return Collections.unmodifiableList(chapters);
    }
}
