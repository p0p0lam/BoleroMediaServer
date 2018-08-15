package ua.com.audioreklama.mediaserver.misc;

import jcifs.SmbResource;

import java.io.IOException;
import java.io.InputStream;

public class ClosableSmbResourceInputStream extends InputStream {
    private InputStream wrapped;
    private SmbResource smbResource;

    public ClosableSmbResourceInputStream(SmbResource resource) throws IOException {
        this.wrapped = resource.openInputStream();
        smbResource = resource;
    }

    @Override
    public int read() throws IOException {
        return wrapped.read();
    }

    @Override
    public void close() throws IOException{
        wrapped.close();
        smbResource.close();
        wrapped=null;
        smbResource=null;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return wrapped.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return wrapped.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        wrapped.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        wrapped.reset();
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }
}
