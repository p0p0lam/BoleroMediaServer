package ua.com.audioreklama.mediaserver.misc;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.SmbResource;
import org.springframework.core.io.AbstractResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class SmbFileResource extends AbstractResource {
    private SmbResource smbResource;
    private String smbFilePath;
    public SmbFileResource(CIFSContext userContext, String smbFilePath) throws IOException {
        smbResource = userContext.get(smbFilePath);
        this.smbFilePath=smbFilePath;
    }

    @Override
    public boolean exists(){
        try {
            return smbResource.exists();
        } catch (CIFSException e) {
            return false;
        }
    }



    @Override
    public File getFile() throws IOException {
        throw new IOException("Get file not supported");
    }

    @Override
    public String getDescription() {
        return "smb file ["+smbFilePath+"]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ClosableSmbResourceInputStream(smbResource);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isFile() {
       return false;
    }

    @Override
    public boolean isReadable() {
        try {
            return smbResource.canRead();
        } catch (CIFSException e) {
            return false;
        }
    }

    @Override
    public String getFilename() {
        return smbResource.getName();
    }

    @Override
    public long contentLength() throws IOException {
        return smbResource.length();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmbFileResource that = (SmbFileResource) o;
        return Objects.equals(smbFilePath, that.smbFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), smbFilePath);
    }
}
