package com.novel.model;

/**
 * 表示小说中的一个章节组
 */
public class Chapter {
    private final int fileIndex;      // 文件索引
    private final int startChapter;   // 起始章节号
    private final int endChapter;     // 结束章节号
    private final String title;       // 章节标题（可选）
    private final int lineNumber;     // 在原文件中的行号（可选）

    public Chapter(int fileIndex, int startChapter, int endChapter) {
        this(fileIndex, startChapter, endChapter, null, -1);
    }

    public Chapter(int fileIndex, int startChapter, int endChapter, String title, int lineNumber) {
        this.fileIndex = fileIndex;
        this.startChapter = startChapter;
        this.endChapter = endChapter;
        this.title = title;
        this.lineNumber = lineNumber;
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

    public String getTitle() {
        return title;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "fileIndex=" + fileIndex +
                ", startChapter=" + startChapter +
                ", endChapter=" + endChapter +
                (title != null ? ", title='" + title + '\'' : "") +
                (lineNumber != -1 ? ", lineNumber=" + lineNumber : "") +
                '}';
    }
}
