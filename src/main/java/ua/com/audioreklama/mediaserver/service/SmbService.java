package ua.com.audioreklama.mediaserver.service;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.CloseableIterator;
import jcifs.SmbResource;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import ua.com.audioreklama.mediaserver.misc.SmbFileResource;
import ua.com.audioreklama.mediaserver.model.network.FileInfo;
import ua.com.audioreklama.mediaserver.model.network.FileListResponse;

import javax.validation.constraints.NotNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
public class SmbService {
    private static final Logger logger = LoggerFactory.getLogger(SmbService.class);
    private static final String DELIVERY_FOLDER="delivery/";
    private static final String RECEIPT_FOLDER="receipt/";
    @Value("${ldap.domain}")
    private String domain;
    @Value("${smb.server.url}")
    private String smbServerUrl;

    @Autowired
    CIFSContext baseContext;


    public long writePackageToStream(String username, String userCN, String password, String packageName, OutputStream out) throws IOException{
        logger.debug("Try to write package {} for user {}", packageName, username);
        CIFSContext userCtx=null;
        try {
            userCtx = getUserContext(username, password);
            try(SmbResource packageResource = userCtx.get(smbServerUrl+getUserFolder(userCN)+"/"+DELIVERY_FOLDER+packageName)){
                if (packageResource.exists() && packageResource.canRead()){
                    try(InputStream is = packageResource.openInputStream()){
                        return StreamUtils.copy(is, out);
                    } catch (IOException e){
                        logger.error("Failed to copy package file " + packageName + " to output stream", e);
                        throw new IOException("Error occurred while coping package " + packageName, e);
                    }
                } else {
                    throw new FileNotFoundException("Package " + packageName + " not found");
                }
            } catch (CIFSException e){
                logger.error("Failed to find package file" + packageName, e);
                throw new FileNotFoundException("Package " + packageName + " not found");
            }
        } finally {
            if (userCtx!=null){
                try {
                    userCtx.close();
                } catch (CIFSException e) {
                    //ignore
                }
            }
        }
    }

    public Resource getPackageAsResource(String username, String userCN, String password, String packageName) throws IOException{
        logger.debug("Try to open package {} for user {}", packageName, username);
        CIFSContext userCtx=null;
        try {
            userCtx = getUserContext(username, password);
            SmbFileResource smbFileResource = new SmbFileResource(userCtx, smbServerUrl+getUserFolder(userCN)+"/"+DELIVERY_FOLDER+packageName);
            logger.debug("Succesfully openned package  {}", packageName);
            return smbFileResource;
        } catch (IOException e){
            logger.error("Failed to open package " + packageName, e);
            if (userCtx!=null){
                try {
                    userCtx.close();
                } catch (CIFSException ee) {
                    //ignore
                }
            }
            throw e;
        }/* finally {
            if (userCtx!=null){
                try {
                    userCtx.close();
                } catch (CIFSException e) {
                    //ignore
                }
            }
        }*/
    }

    public Resource getPackageAsIsResource(String username, String userCN, String password, String packageName) throws IOException{
        logger.debug("Try to open package {} for user {}", packageName, username);
        CIFSContext userCtx= getUserContext(username, password);
        try(SmbResource r = userCtx.get(smbServerUrl + getUserFolder(userCN) + "/" + DELIVERY_FOLDER + packageName)){
            if (r.exists() && r.canRead()) {
                logger.debug("package {} found", packageName);
                return new InputStreamResource(r.openInputStream(), r.getName());
            } else {
                logger.error("Package {} not found", packageName);
                throw new FileNotFoundException("Package " + packageName +" not found");
            }
        } finally {
            userCtx.close();
        }

    }


    public List<FileInfo> listDelivery(String username, String userCN, String password){
        CIFSContext userCtx=null;
        try {

            userCtx = getUserContext(username, password);
            try (SmbResource r = userCtx.get(smbServerUrl+getUserFolder(userCN)+"/"+DELIVERY_FOLDER)) {
                logger.debug("Delivery folder exists: {}", r.exists());
                if (r.exists() && r.isDirectory()) {
                    Iterator<SmbResource> childs = r.children();
                    List<FileInfo> files = new ArrayList<>();
                    while (childs.hasNext()) {
                        try(SmbResource next = childs.next()){
                            if (next.isFile()){
                                FileInfo fileInfo = new FileInfo();
                                fileInfo.setName(next.getName());
                                fileInfo.setLastModified(next.lastModified());
                                files.add(fileInfo);
                            }
                        }
                    }
                    return files;
                } else {
                    logger.error("{} Folder not found for user {}", DELIVERY_FOLDER, username);
                }
            }
        } catch (CIFSException e) {
            logger.error("Failed to get smb folder for user", e);
        } finally {
            try {
                userCtx.close();
            } catch (CIFSException e) {
                //e.printStackTrace();
            }
        }
        return Collections.emptyList();
    }


    private String getUserFolder(@NotNull String cn){
        String lowerCaseCn = cn.toLowerCase();
        return lowerCaseCn.replaceAll(" ", "_");
    }
    private CIFSContext getUserContext(String username, String password){
        return baseContext.withCredentials(
                new NtlmPasswordAuthenticator(domain, username, password)
        );
    }

    public void testListFiles(String username, String userCN, String password){
        CIFSContext userCtx=null;
        try {

            userCtx = getUserContext(username, password);
            try (SmbResource r = userCtx.get(smbServerUrl)) {
                logger.debug("Resource exists: {}", r.exists());
                if (r.exists()) {
                    logger.debug("Root: {}", r.getName());
                    listChilds(r, 0);
                }
            }
            //downloadFiles(userCtx, "dnepr__geroev__bs/delivery/");
        } catch (CIFSException e) {
            logger.error("Failed to get smb share", e);
        } finally {
            try {
                userCtx.close();
            } catch (CIFSException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadFiles(SmbResource r){
        try{
            if (r.isDirectory()){
                Iterator<SmbResource> childs = r.children();
                while (childs.hasNext()) {
                    SmbResource next = childs.next();
                    if (next.isFile() && !next.getName().endsWith(".tmp")){
                        Path targetFolder = Paths.get(r.getLocator().getURLPath());
                        Files.createDirectories(targetFolder);
                        logger.debug("Saving file {} to {}", next.getName(), targetFolder.toUri());
                        try(InputStream is = next.openInputStream()) {
                            Files.copy(is, targetFolder.resolve(next.getName()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            } else if (!r.getName().endsWith(".tmp")) {
                Path targetFolder = Paths.get(r.getLocator().getURLPath());
                Files.createDirectories(targetFolder);
                logger.debug("Saving file {} to {}", r.getName(), targetFolder.toUri());
                try(InputStream is = r.openInputStream()) {
                    Files.copy(is, targetFolder.resolve(r.getName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (CIFSException e){
            logger.error("Failed to download files from " + r.getLocator().getURLPath(), e);
        } catch (IOException e) {
            logger.error("Failed to save file from " + r.getLocator().getURLPath(), e);
        }
    }

    private void listChilds(SmbResource parent, int level) throws CIFSException {
        if (parent.getLocator().getURLPath().endsWith("/receipt/")){
            downloadFiles(parent);
        }
        Iterator<SmbResource> childs = parent.children();
        while (childs.hasNext()) {
            SmbResource next = childs.next();
            StringBuilder spaces = new StringBuilder();
            for (int i=0; i<=level; i++){
                spaces.append(" ");
            }
            if (next.isFile()){
                logger.debug("{}{} size[{}]", spaces, next.getName(), next.length());
            } else {
                logger.debug("{}{}", spaces, next.getName());
            }
            if (next.isDirectory()){
                listChilds(next, level+1);
            }
        }
    }
}
