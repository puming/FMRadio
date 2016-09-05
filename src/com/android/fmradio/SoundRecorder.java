package com.android.fmradio;

/**
 * Created by puming1 on 4/25/16.
 */
public class SoundRecorder {
    private String fileName;
    private String path;
    private String author;
    private boolean isOpen;

    public SoundRecorder() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setIsOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
