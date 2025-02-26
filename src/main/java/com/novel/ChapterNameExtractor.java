package com.novel;

import com.novel.util.Constants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 章节名称提取器
 */
public class ChapterNameExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ChapterNameExtractor.class);
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("第.{1,5}章");

    public static void main(String[] args) {
        Path novelFilePath = Paths.get(Constants.NOVEL_FILE_PATH);
        File novelFile = novelFilePath.toFile();

        File outputFile = new File(Paths.get(System.getProperty("user.dir"), "章节名称.txt").toString());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(novelFile), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            int matchCount = 0;

            logger.info("开始提取章节名称...");
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber % 1000 == 0) {
                    logger.debug("正在处理第{}行", lineNumber);
                }

                Matcher matcher = CHAPTER_PATTERN.matcher(line);
                if (matcher.find()) {
                    writer.write(line);
                    writer.newLine();
                    matchCount++;
                }
            }

            logger.info("章节名称提取完成，共找到{}个匹配项", matchCount);
            logger.info("结果已保存到: {}", outputFile.getAbsolutePath());

        } catch (IOException e) {
            logger.error("处理文件时出错", e);
        }
    }
}
