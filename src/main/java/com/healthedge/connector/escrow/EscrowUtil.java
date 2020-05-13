package com.healthedge.connector.escrow;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class EscrowUtil {

	/** Test if a trimmed String is empty or null */
	public static boolean isNullOrTrimmedEmpty(String s) {
		return s == null || s.trim().length() == 0;
	}

	/**
	 * Gets current date.
	 *
	 * @return String
	 *
	 * @throws Exception
	 */
	public static String getCurrentDateAsYYYYMMdd() throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
		return LocalDate.now().format(formatter);
	}

	public static String getCurrentDateAsText() throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM, dd yyyy");
		return LocalDate.now().format(formatter);
	}

	public static Properties loadProp(String tempPath) throws Exception{
		Properties ret = new Properties();
		Path p = Paths.get(tempPath + "/escrow.properties");
		if (Files.exists(p)) {
			ret.load(Files.newInputStream(p));
		}
		return ret;
	}

	/**
	 * Utility method to send simple HTML email
	 * @param properties
	 */
	public static void sendEmail(Properties properties, String docPath,
								 String depositName, List<Path> fileNames) throws Exception{
		try
		{
			String from = properties.getProperty("DOWNLOAD_USER") != null ? properties.getProperty("DOWNLOAD_USER") : "";
			Properties props = System.getProperties();
			props.put("mail.smtp.host", properties.getProperty("SMTPHOST"));
			props.put("mail.smtp.port", properties.getProperty("SMTPPORT"));
			//props.put("mail.debug", "true");
			Session session = Session.getInstance(props, null);

			MimeMessage msg = new MimeMessage(session);
			//set message headers
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");

			msg.setFrom(new InternetAddress("no-reply@healthedge.com"));
			msg.setSubject("Deposit Today - " + depositName, "UTF-8");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("IPMElectronicDepositing@ironmountain.com", false));
			//msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse("jtripathy@healthedge.com", false));
			InternetAddress[] ccAddrs = new InternetAddress[2];
			ccAddrs[0] = new InternetAddress("mdorros@healthedge.com", false);
			if(!isNullOrTrimmedEmpty(from)){
				ccAddrs[1] = new InternetAddress(from+"@healthedge.com", false);
			}
			msg.setRecipients(Message.RecipientType.CC, ccAddrs);

			Multipart multipart = new MimeMultipart();
			// creates body part for the message
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			StringBuilder sb = new StringBuilder();
			sb.append("Please see the attached deposit for account #"+ properties.getProperty("SFTPWORKINGDIR") +":" +
					"<br>" +
					"<br>");
			for (Path file : fileNames) {
				sb.append("     " + file.getFileName() + "<br>");
			}
			sb.append("<br>" +
					"<br>" +
					"Thanks," +
					"<br>" +
					from);
			//msg.setText(sb.toString(), "UTF-8");
			messageBodyPart.setContent(sb.toString(), "text/HTML; charset=UTF-8");

			// creates body part for the attachment
			MimeBodyPart attachPart = new MimeBodyPart();
			attachPart.attachFile(docPath);

			multipart.addBodyPart(messageBodyPart);
			multipart.addBodyPart(attachPart);
			// sets the multipart as message's content
			msg.setContent(multipart);

			Transport.send(msg);
			System.out.println("EMail Sent Successfully!!");
		}
		catch (Exception e) {
			System.out.println("Error in sending email.");
			throw e;
		}
	}
}