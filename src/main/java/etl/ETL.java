package etl;

import java.io.File;

import utility.ContentPostProcessing;

/**
 * Processing and input file to Solr
 * 
 * @author tnhung
 */
public class ETL {
	public String processing(File file) {
		OCR ocr = new OCR(file);
		String content = ocr.processing();
		content = ContentPostProcessing.removeRedundantNewLine(content);
		content = ContentPostProcessing.replaceMisOCR(content);
		//ocr.removeTemp();
		return content;
	}

}
