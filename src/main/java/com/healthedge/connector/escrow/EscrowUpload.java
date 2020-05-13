package com.healthedge.connector.escrow;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.List;
import java.util.Properties;

/**
 * Created by jtripathy
 */
public class EscrowUpload {

    public  void send (List<Path> fileNames, Properties properties) throws Exception{
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        System.out.println("preparing the host information for sftp.");
        try {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
            JSch jsch = new JSch();
            String host = properties.getProperty("SFTPHOST");
            String port = properties.getProperty("SFTPPORT");
            System.out.println("Connecting to " + host + " on port " + port);
            session = jsch.getSession(properties.getProperty("SFTPUSER"), host, Integer.parseInt(port));
            session.setPassword(properties.getProperty("SFTPPSWD"));
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            System.out.println("Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            System.out.println("sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;
            channelSftp.cd(properties.getProperty("SFTPWORKINGDIR"));
            for(Path file : fileNames){
                channelSftp.put(Files.newInputStream(file), file.getFileName().toString());
            }
            System.out.println("File transfered successfully to host.");
        } catch (Exception ex) {
            System.out.println("Exception found while transfer the files to iron mountain.");
            throw ex;
        }
        finally{
            try {
                if(channelSftp != null) {
                    channelSftp.exit();
                    System.out.println("sftp Channel exited.");
                }
                if(channel != null) {
                    channel.disconnect();
                    System.out.println("Channel disconnected.");
                }
                if(session != null) {
                    session.disconnect();
                    System.out.println("Host Session disconnected.");
                }
            }catch (Exception ex){}
        }
    }
}
