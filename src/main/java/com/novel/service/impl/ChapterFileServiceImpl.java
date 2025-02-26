package com.novel.service.impl;

import com.novel.model.Chapter;
import com.novel.service.ChapterFileService;
import com.novel.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterFileServiceImpl implements ChapterFileService {
    private static final Logger logger = LoggerFactory.getLogger(ChapterFileServiceImpl.class);
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("(\\d+)第(\\d+)-(\\d+)章\\.txt");

    @Override
    public String readChapterContent(File file) throws Exception {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    @Override
    public void saveAnalysisResult(String fileName, String content) throws Exception {
        Path outputPath = Paths.get(Constants.API_RESULTS_DIR, fileName);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        logger.info("已保存分析结果: {}", fileName);
    }

    @Override
    public List<File> getAllChapterFiles() throws Exception {
        File splitChaptersDir = new File(Constants.SPLIT_CHAPTERS_DIR);
        if (!splitChaptersDir.exists() || !splitChaptersDir.isDirectory()) {
            throw new IllegalStateException("分割章节目录不存在: " + Constants.SPLIT_CHAPTERS_DIR);
        }

        File[] files = splitChaptersDir.listFiles((dir, name) -> name.matches("\\d+第\\d+-\\d+章\\.txt"));
        if (files == null || files.length == 0) {
            throw new IllegalStateException("没有找到分割后的章节文件");
        }

        // 按文件名排序
        Arrays.sort(files, Comparator.comparing(File::getName));
        return Arrays.asList(files);
    }

    @Override
    public Chapter parseChapterInfo(File file) {
        Matcher matcher = FILE_NAME_PATTERN.matcher(file.getName());
        if (matcher.find()) {
            int fileIndex = Integer.parseInt(matcher.group(1));
            int startChapter = Integer.parseInt(matcher.group(2));
            int endChapter = Integer.parseInt(matcher.group(3));
            return new Chapter(fileIndex, startChapter, endChapter);
        }
        throw new IllegalArgumentException("无效的文件名格式: " + file.getName());
    }
}
