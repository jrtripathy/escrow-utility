package com.healthedge.connector.escrow;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Text;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EscrowUtility {
	private static final String TEMPLATE_NAME = "IronMountain_YYYYMMDD_HealthEdge.docx";

	public static void main(String[] args) throws Exception {
		new EscrowUtility().process(args);
	}

	/**
	 * Main processing.
	 *
	 * @param args String[]
	 *
	 * @throws Exception
	 */
	public void process(String[] args) throws Exception {
		if (args.length < 1) {
			throw new Exception(getUsage());
		} else {
			String tempDir = args[0];
			System.out.println("TEMP DIR: " + tempDir);
			String templatePath = tempDir + "/" + TEMPLATE_NAME;
			String fileName = TEMPLATE_NAME.replace("YYYYMMDD" , EscrowUtil.getCurrentDateAsYYYYMMdd());
			String docPath = tempDir + "/" + fileName;
			Properties properties = EscrowUtil.loadProp(tempDir);
			//System.out.println("Properties: " + properties);

			String uploadDir = tempDir + "/upload";
			//copy zipped source from windows shared folder to local eg CareAdmin
			if(!EscrowUtil.isNullOrTrimmedEmpty(properties.getProperty("NETWORK_FILE"))) {
				EscrowNetworkAccess networkAccess = new EscrowNetworkAccess();
				networkAccess.copyCareAdmin(uploadDir, properties);
			}

			String depositName = properties.getProperty("DEPOSIT_NAME");
			String releaseVersion = properties.getProperty("RELEASE_VERSION");
			List<Path> fileNames = new ArrayList<>();
			for (Path file : Files.newDirectoryStream(Paths.get(uploadDir).toAbsolutePath(), path -> path.toFile().isFile())) {
				String tempName = file.getFileName().toString();
				if (tempName.endsWith(".tgz") || tempName.endsWith(".zip") || tempName.endsWith(".tar.gz")
						|| tempName.endsWith(".txt")) {
					fileNames.add(file);
					if(tempName.matches("CA.*\\.zip")){
						depositName += " and CareAdmin " + tempName.substring(tempName.indexOf("-")+1, tempName.indexOf("_"));
					}
				}
			}
			System.out.println("Files to Upload: " + fileNames);
			System.out.println("depositName: " + depositName);
			//Updating the template
			updateTemplate(templatePath, depositName, releaseVersion, docPath, String.valueOf(fileNames.size()));
			//uploading files
			EscrowUpload escrowUpload = new EscrowUpload();
			escrowUpload.send(fileNames, properties);
			//sending email
			EscrowUtil.sendEmail(properties, docPath, depositName, fileNames);
		}
	}

	private void updateTemplate(String templatePath,
								String depositName,
								String releaseVersion,
								String docPath,
								String size) throws Exception{
		System.out.println("Updating template and creating word doc " + templatePath);
		WordprocessingMLPackage template = getTemplate(templatePath);
		List<Object> texts = getAllElementFromObject(template.getMainDocumentPart(), Text.class);
		replacePlaceholder(texts, depositName, "*");
		replacePlaceholder(texts, releaseVersion, "!");
		replacePlaceholder(texts, size, "$");
		replacePlaceholder(texts, EscrowUtil.getCurrentDateAsText(), "&");

		writeDocxToStream(template, docPath);
		System.out.println("doc created.");
	}

	private WordprocessingMLPackage getTemplate(String name) throws Docx4JException, FileNotFoundException {
		WordprocessingMLPackage template = WordprocessingMLPackage.load(new FileInputStream(new File(name)));
		return template;
	}

	private static List<Object> getAllElementFromObject(Object obj, Class<?> toSearch) {
		List<Object> result = new ArrayList<Object>();
		if (obj instanceof JAXBElement) obj = ((JAXBElement<?>) obj).getValue();

		if (obj.getClass().equals(toSearch))
			result.add(obj);
		else if (obj instanceof ContentAccessor) {
			List<?> children = ((ContentAccessor) obj).getContent();
			for (Object child : children) {
				result.addAll(getAllElementFromObject(child, toSearch));
			}

		}
		return result;
	}

	private void replacePlaceholder(List<Object> texts, String name, String placeholder ) {
		for (Object text : texts) {
			Text textElement = (Text) text;
			//System.out.println("Value: " + textElement.getValue() + ", Space: " + textElement.getSpace());
			if (placeholder.equalsIgnoreCase(textElement.getValue())) {
				textElement.setValue(name);
			}
		}
	}

	private void writeDocxToStream(WordprocessingMLPackage template, String target)
			throws IOException, Docx4JException {
		File f = new File(target);
		template.save(f);
	}

	private String getReleaseVersion(Properties properties){
		String releaseVersion = properties.getProperty("C4_VERSION");
		DefaultArtifactVersion c4Version = new DefaultArtifactVersion(properties.getProperty("C4_VERSION"));
		DefaultArtifactVersion classicVersion = new DefaultArtifactVersion(properties.getProperty("CLASSIC_VERSION"));
		if (c4Version.compareTo(classicVersion) < 0 ) {
			releaseVersion = properties.getProperty("CLASSIC_VERSION");
		}
		return releaseVersion;
	}
	/**
	 * Prints Usage.
	 *
	 * @return String
	 */
	private String getUsage() {
		StringBuffer sb = new StringBuffer();
		sb.append("Usage:");
		sb.append("\n\t");
		sb.append("java -jar connector-escrow-utility-<VERSION>.jar <PATH_OF_TEMP_DIRECTORY>");
		return sb.toString();
	}
}