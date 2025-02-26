package com.novel.service;

import com.novel.model.Chapter;
import java.io.File;
import java.util.List;

public interface ChapterFileService {
    /**
     * 读取章节文件内容
     */
    String readChapterContent(File file) throws Exception;

    /**
     * 保存API分析结果
     */
    void saveAnalysisResult(String fileName, String content) throws Exception;

    /**
     * 获取所有章节文件
     */
    List<File> getAllChapterFiles() throws Exception;

    /**
     * 解析文件名中的章节信息
     */
    Chapter parseChapterInfo(File file);
}
