package ua.com.audioreklama.mediaserver.model.network;

import java.util.List;

public class FileListResponse {
    private List<FileInfo> files;

    public FileListResponse(List<FileInfo> files) {
        this.files = files;
    }

    public List<FileInfo> getFiles() {
        return files;
    }
}
