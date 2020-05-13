package com.healthedge.connector.escrow;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Created by jtripathy
 */
public class EscrowNetworkAccess {

    public  void copyCareAdmin(String tempDir, Properties properties) throws Exception{
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("Headquarters",
                properties.getProperty("DOWNLOAD_USER"), properties.getProperty("AD_PSWD"));

        String[] networkFiles = properties.getProperty("NETWORK_FILE").split(",");
        for(String fileName : networkFiles) {
            if(!EscrowUtil.isNullOrTrimmedEmpty(fileName)) {
                InputStream bis = null;
                OutputStream bos = null;
                try {
                    String path = "smb:" + properties.getProperty("NETWORK_FOLDER") + File.separator + fileName;
                    System.out.println("Path: " + path);
                    byte[] buffer = new byte[8 * 1024 * 1024];
                    int bytesRead;
                    SmbFile rmifile = new SmbFile(path, auth);
                    File localfile = new File(tempDir + File.separator + rmifile.getName());
                    bis = new SmbFileInputStream(rmifile);
                    bos = new FileOutputStream(localfile);
                    while ((bytesRead = bis.read(buffer)) > 0) {
                        System.out.print("ok\n");
                        bos.write(buffer, 0, bytesRead);
                    }
                    System.out.println("Downloaded file " + fileName);
                } catch (Exception e) {
                    System.out.println("Unable to copy file from samba to local file : " + e.getMessage());
                    throw e;
                } finally {
                    try {
                        if (bos != null) {
                            bos.close();
                        }
                        if (bis != null) {
                            bis.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}
