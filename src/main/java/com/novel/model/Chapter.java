package com.novel.model;

/**
 * 表示小说中的一个章节
 */
public class Chapter {
    private final int number;
    private final String title;
    private final int lineNumber;

    public Chapter(int number, String title, int lineNumber) {
        this.number = number;
        this.title = title;
        this.lineNumber = lineNumber;
    }

    public int getNumber() {
        return number;
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
                "number=" + number +
                ", title='" + title + '\'' +
                ", lineNumber=" + lineNumber +
                '}';
    }
}
