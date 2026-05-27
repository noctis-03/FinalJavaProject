package com.blople.model.block;

import com.blople.model.enums.BlockType;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * 파일 블록 (FR-05).
 */
public class FileBlock extends Block {

    private String subjectId;
    private String filePath;
    private String fileName;
    private String fileType;   // 확장자 또는 MIME
    private long   fileSize;

    public FileBlock(String subjectId, String name, String color,
                     String filePath, String fileName, String fileType, long fileSize) {
        super(BlockType.FILE, name, color);
        this.subjectId = subjectId;
        this.filePath  = filePath;
        this.fileName  = fileName;
        this.fileType  = fileType;
        this.fileSize  = fileSize;
    }

    public String getSubjectId()                  { return subjectId; }
    public void setSubjectId(String subjectId)    { this.subjectId = subjectId; touch(); }
    public String getFilePath()                   { return filePath; }
    public void setFilePath(String filePath)      { this.filePath = filePath; touch(); }
    public String getFileName()                   { return fileName; }
    public void setFileName(String fileName)      { this.fileName = fileName; touch(); }
    public String getFileType()                   { return fileType; }
    public void setFileType(String fileType)      { this.fileType = fileType; touch(); }
    public long getFileSize()                     { return fileSize; }
    public void setFileSize(long fileSize)        { this.fileSize = fileSize; touch(); }

    /** 시스템 기본 프로그램으로 파일 열기. */
    public void openFile() throws IOException {
        File f = new File(filePath);
        if (!f.exists()) {
            throw new IOException("파일을 찾을 수 없습니다: " + filePath);
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("이 OS에서 Desktop API를 지원하지 않습니다.");
        }
        Desktop.getDesktop().open(f);
    }

    public boolean isFileValid() {
        return filePath != null && new File(filePath).exists();
    }
}
