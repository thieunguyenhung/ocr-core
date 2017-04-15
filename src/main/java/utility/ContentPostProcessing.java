package utility;

/**
 * Post processing content of file before input to Solr
 * 
 * @author tnhung
 */
public class ContentPostProcessing {

	/**
	 * Remove all triple consecutive newlines by double newlines</br>
	 * "<b>\n \n \n</b>" --> "<b>\n \n</b>"
	 * 
	 * @param content
	 *            <b>String</b> string to remove redundant newlines
	 * @return <b>String</b>
	 */
	public static String removeRedundantNewLine(String content) {
		content = content.replaceAll("\n \n", "\n");
		while (content.contains("\n\n\n")) {
			content = content.replaceAll("\n\n\n", "\n\n");
		}
		return content;
	}

	/**
	 * Replace all mis-ocr word to correct in Vietnamese
	 * 
	 * @param content
	 *            <b>String</b> string to replace mis-ocr word
	 * @return <b>String</b>
	 */
	public static String replaceMisOCR(String content) {
		content = content.replaceAll("tmg", "úng");
		content = content.replaceAll("rl", "n");
		content = content.replaceAll("rr", "n");
		content = content.replaceAll("êĩ-", "ết");
		content = content.replaceAll("âỳ", "ấy");
		content = content.replaceAll("oĩ", "ơi");
		content = content.replaceAll("ôỈ", "ỡi");
		content = content.replaceAll("u1I", "ưn");
		content = content.replaceAll("q1I", "qu");
		content = content.replaceAll("êf", "ết");
		content = content.replaceAll("fâ", "rầ");
		content = content.replaceAll("fê", "iề");
		content = content.replaceAll("ýê", "yề");
		content = content.replaceAll("lc", "k");
		content = content.replaceAll("vđi", "với");
		content = content.replaceAll("cũa", "của");
		content = content.replaceAll("phãi", "phải");
		content = content.replaceAll("âÍ", "ấ");
		content = content.replaceAll("––", "—");
		content = content.replaceAll("—-", "—");
		content = content.replaceAll("-—", "—");
		return content;
	}
}
