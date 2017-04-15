package etl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import net.sourceforge.tess4j.TesseractException;

public class OCR {

	private String fileName;
	private String path;
	private String content;

	public OCR(File file) {
		fileName = file.getName();
		path = FilenameUtils.getFullPathNoEndSeparator(file.getAbsolutePath());
	}

	public String processing() {
		try {
			doConvertToPNG();
			doOCRFiles();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//removeTemp();
		}
		return content;

	}

	private void doConvertToPNG() throws IOException {
		// create command
		ConvertCmd cmd = new ConvertCmd();

		// create the operation, add images and operators/options
		IMOperation op = new IMOperation();

		op.colorspace("sRGB");
		op.profile(System.getProperty("user.dir") + "/tessdata/adobe.icc");
		op.density(150);
		op.units("PixelsPerInch");
		op.addImage(path + "/" + fileName);
		op.depth(8);
		op.addImage(path + "/" + fileName + ".png");

		// execute the operation
		try {
			cmd.run(op);
		} catch (InterruptedException | IM4JavaException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Convert complete!!");
		}

	}

	private void doOCRFiles() {
		File dir = new File(path);
		File[] listOfFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(fileName) && name.toLowerCase().endsWith(".png");
			}
		});

		Arrays.sort(listOfFiles);

		content = "";
		int i = 0;
		for (File file : listOfFiles) {
			try {
				System.out.println("Do OCR file " + file.getName());

				ConvTesseract instance = new ConvTesseract();

				if (i == 0)
					instance.setFirstPage(true);

				if (i == listOfFiles.length - 1)
					instance.setLastPage(true);
				
				String result = instance.doOCR(file);
				content += "\n" + result;
				i++;
			} catch (TesseractException e) {
				e.printStackTrace();
			}
		}
	}

	public void removeTemp() {
		File dir = new File(path);
		File[] listOfFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(fileName) && name.toLowerCase().endsWith(".png");
			}
		});

		for (File file : listOfFiles) {
			file.delete();
		}
	}

	/**
	 * Get file content in <b>List&#60;String&#62</b>
	 * 
	 * @return List&#60;String&#62
	 */
	public List<String> getContentInList() {
		return Arrays.asList(content.split("\\r?\\n"));
	}

}
