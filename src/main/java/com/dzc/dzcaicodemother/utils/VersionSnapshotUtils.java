package com.dzc.dzcaicodemother.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 版本快照数据工具类
 *
 * @author dzc
 */
public class VersionSnapshotUtils {

    /**
     * 从目录创建快照数据
     *
     * @param directoryPath 目录路径
     * @return 快照数据JSON字符串
     */
    public static String createSnapshotFromDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        List<FileInfo> files = new ArrayList<>();
        collectFiles(directory, directory, files);

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setFiles(files);

        return JSONUtil.toJsonStr(snapshotData);
    }

    /**
     * 递归收集文件
     */
    private static void collectFiles(File rootDir, File currentDir, List<FileInfo> files) {
        File[] fileList = currentDir.listFiles();
        if (fileList == null) {
            return;
        }

        for (File file : fileList) {
            if (file.isDirectory()) {
                collectFiles(rootDir, file, files);
            } else {
                String relativePath = getRelativePath(rootDir, file);
                String content = FileUtil.readString(file, StandardCharsets.UTF_8);

                FileInfo fileInfo = new FileInfo();
                fileInfo.setPath(relativePath);
                fileInfo.setContent(content);
                files.add(fileInfo);
            }
        }
    }

    /**
     * 获取相对路径
     */
    private static String getRelativePath(File rootDir, File file) {
        String rootPath = rootDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        return filePath.substring(rootPath.length() + 1).replace("\\", "/");
    }

    /**
     * 快照数据结构
     */
    private static class SnapshotData {
        private List<FileInfo> files;

        public List<FileInfo> getFiles() {
            return files;
        }

        public void setFiles(List<FileInfo> files) {
            this.files = files;
        }
    }

    /**
     * 文件信息
     */
    private static class FileInfo {
        private String path;
        private String content;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

}
