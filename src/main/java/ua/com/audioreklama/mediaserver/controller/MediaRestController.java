package ua.com.audioreklama.mediaserver.controller;

import com.sun.security.auth.LdapPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.audioreklama.mediaserver.model.network.FileInfo;
import ua.com.audioreklama.mediaserver.model.network.FileListResponse;
import ua.com.audioreklama.mediaserver.service.SmbService;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@RestController
public class MediaRestController {
    private static final Logger logger = LoggerFactory.getLogger(MediaRestController.class);
    @Autowired
    SmbService smbService;

    @GetMapping("/delivery")
    public FileListResponse getDeliveryFiles(Authentication auth){
        String userName = auth.getName();
        String password = (String) auth.getCredentials();
        String cn =null;
        if (auth.getPrincipal() instanceof LdapUserDetails) {
            LdapUserDetails userDetails = (LdapUserDetails)auth.getPrincipal();
            cn = getCn(userDetails.getDn());
        }
        List<FileInfo> deliveryFiles = smbService.listDelivery(userName, StringUtils.isEmpty(cn)?userName:cn, password);
        FileListResponse response = new FileListResponse(deliveryFiles);
        return response;
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadPackage(@RequestParam(name = "packageName") String packageName, Authentication auth, HttpServletResponse response){
        String userName = auth.getName();
        String password = (String) auth.getCredentials();
        String cn =null;
        if (auth.getPrincipal() instanceof LdapUserDetails) {
            LdapUserDetails userDetails = (LdapUserDetails)auth.getPrincipal();
            cn = getCn(userDetails.getDn());
        }
        Resource resource;
        try {
            resource = smbService.getPackageAsResource(userName, StringUtils.isEmpty(cn)?userName:cn, password, packageName);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" +resource.getFilename() + "\"").body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }

    }

    @GetMapping("/")
    public String index(Authentication auth){
        String userName = auth.getName();
        String password = (String) auth.getCredentials();
        String cn =userName;
        if (auth.getPrincipal() instanceof LdapUserDetails) {
            LdapUserDetails userDetails = (LdapUserDetails)auth.getPrincipal();
            String tmp = getCn(userDetails.getDn());
            if (!StringUtils.isEmpty(tmp)){
                cn = tmp;
            }
        }
        smbService.testListFiles(userName, cn, password);
        return "Welcome";
    }

    private String getCn(String dn){
        LdapName ln = LdapUtils.newLdapName(dn);
        for(Rdn rdn : ln.getRdns()) {
            if(rdn.getType().equalsIgnoreCase("CN")) {
               logger.debug("CN is: '{}'",  rdn.getValue());
                return (String) rdn.getValue();
            }
        }
        return null;
    }
}
