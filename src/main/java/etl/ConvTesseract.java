/**
 * Copyright @ 2017 Phu Truong
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package etl;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.PointerByReference;

import etl.core.RedRemover;

import java.awt.Rectangle;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;

import net.sourceforge.lept4j.Box;
import net.sourceforge.lept4j.Boxa;
import static net.sourceforge.lept4j.ILeptonica.L_CLONE;
import net.sourceforge.lept4j.Leptonica;
import static net.sourceforge.tess4j.ITessAPI.TRUE;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITessAPI.TessBaseAPI;
import net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode;
import net.sourceforge.tess4j.ITessAPI.TessPageIterator;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.ITessAPI.TessResultIterator;
import net.sourceforge.tess4j.ITessAPI.TessResultRenderer;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.tess4j.util.LoggHelper;
import net.sourceforge.tess4j.util.PdfUtilities;
import org.slf4j.*;

/**
 * An object layer on top of <code>TessAPI</code>, provides character
 * recognition support for common image formats, and multi-page TIFF images
 * beyond the uncompressed, binary TIFF format supported by Tesseract OCR
 * engine. The extended capabilities are provided by the
 * <code>Java Advanced Imaging Image I/O Tools</code>.<br>
 * <br>
 * Support for PDF documents is available through <code>Ghost4J</code>, a
 * <code>JNA</code> wrapper for <code>GPL Ghostscript</code>, which should be
 * installed and included in system path.<br>
 * <br>
 * Any program that uses the library will need to ensure that the required
 * libraries (the <code>.jar</code> files for <code>jna</code>,
 * <code>jai-imageio</code>, and <code>ghost4j</code>) are in its compile and
 * run-time <code>classpath</code>.
 */
public class ConvTesseract implements ITesseract {

	private static ConvTesseract instance;
	private String language = "vie";
	private String datapath = System.getProperty("user.dir");
	private RenderedFormat renderedFormat = RenderedFormat.TEXT;
	private int psm = 3;
	private int ocrEngineMode = TessOcrEngineMode.OEM_DEFAULT;
	private final Properties prop = new Properties();
	private final List<String> configList = new ArrayList<String>();
	private Lenet lenet = null;
	private boolean firstPage = false;
	private boolean lastPage = false;

	private TessAPI api;
	private TessBaseAPI handle;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

	public ConvTesseract() {
		super();

		init();
		setTessVariables();
	}

	public void setFirstPage(boolean firstPage) {
		this.firstPage = firstPage;
	}

	public void setLastPage(boolean lastPage) {
		this.lastPage = lastPage;
	}

	/**
	 * Returns TessAPI object.
	 *
	 * @return api
	 */
	protected TessAPI getAPI() {
		return api;
	}

	/**
	 * Returns API handle.
	 *
	 * @return handle
	 */
	protected TessBaseAPI getHandle() {
		return handle;
	}

	/**
	 * Gets an instance of the class library.
	 *
	 * @deprecated As of Release 2.0, use default constructor instead.
	 * @return instance
	 */
	@Deprecated
	public static synchronized ConvTesseract getInstance() {
		if (instance == null) {
			instance = new ConvTesseract();
		}

		return instance;
	}

	/**
	 * Sets path to <code>tessdata</code>.
	 *
	 * @param datapath
	 *            the tessdata path to set
	 */
	@Override
	public void setDatapath(String datapath) {
		this.datapath = datapath;
	}

	/**
	 * Sets language for OCR.
	 *
	 * @param language
	 *            the language code, which follows ISO 639-3 standard.
	 */
	@Override
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Sets OCR engine mode.
	 *
	 * @param ocrEngineMode
	 *            the OcrEngineMode to set
	 */
	@Override
	public void setOcrEngineMode(int ocrEngineMode) {
		this.ocrEngineMode = ocrEngineMode;
	}

	/**
	 * Sets page segmentation mode.
	 *
	 * @param mode
	 *            the page segmentation mode to set
	 */
	@Override
	public void setPageSegMode(int mode) {
		this.psm = mode;
	}

	/**
	 * Enables hocr output.
	 *
	 * @param hocr
	 *            to enable or disable hocr output
	 */
	public void setHocr(boolean hocr) {
		this.renderedFormat = hocr ? RenderedFormat.HOCR : RenderedFormat.TEXT;
		prop.setProperty("tessedit_create_hocr", hocr ? "1" : "0");
	}

	/**
	 * Set the value of Tesseract's internal parameter.
	 *
	 * @param key
	 *            variable name, e.g., <code>tessedit_create_hocr</code>,
	 *            <code>tessedit_char_whitelist</code>, etc.
	 * @param value
	 *            value for corresponding variable, e.g., "1", "0",
	 *            "0123456789", etc.
	 */
	@Override
	public void setTessVariable(String key, String value) {
		prop.setProperty(key, value);
	}

	/**
	 * Sets configs to be passed to Tesseract's <code>Init</code> method.
	 *
	 * @param configs
	 *            list of config filenames, e.g., "digits", "bazaar", "quiet"
	 */
	@Override
	public void setConfigs(List<String> configs) {
		configList.clear();
		if (configs != null) {
			configList.addAll(configs);
		}
	}

	/**
	 * Performs OCR operation.
	 *
	 * @param imageFile
	 *            an image file
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(File imageFile) throws TesseractException {
		return doOCR(imageFile, null);
	}

	/**
	 * Performs OCR operation.
	 *
	 * @param imageFile
	 *            an image file
	 * @param rect
	 *            the bounding rectangle defines the region of the image to be
	 *            recognized. A rectangle of zero dimension or <code>null</code>
	 *            indicates the whole image.
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(File imageFile, Rectangle rect) throws TesseractException {

		StringBuilder sb = new StringBuilder();
		try {
			BufferedImage bi = ImageIO.read(imageFile);
			setImage(bi, rect);
			sb.append(getOCRText(imageFile.getPath()));
		} catch (IOException ioe) {
			// skip the problematic image
			logger.error(ioe.getMessage(), ioe);
		}

		return sb.toString();
	}

	/**
	 * Performs OCR operation.
	 *
	 * @param bi
	 *            a buffered image
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(BufferedImage bi) throws TesseractException {
		String str = "";
		try {

			System.out.println(bi.getHeight() + " - " + bi.getWidth());

			setImage(bi, null);
			str = getOCRText();

			System.out.println(str);

		} catch (IOException ioe) {
			// skip the problematic image
			logger.error(ioe.getMessage(), ioe);
		} finally {
			dispose();
		}

		return str;
	}

	protected String getOCRText() {
		List<Word> words = getSymbols();
		System.out.println(words);
		Pointer utf8Text = api.TessBaseAPIGetUTF8Text(handle);
		String str = utf8Text.getString(0);
		api.TessDeleteText(utf8Text);
		return str;
	}

	/**
	 * Performs OCR operation.
	 *
	 * @param bi
	 *            a buffered image
	 * @param rect
	 *            the bounding rectangle defines the region of the image to be
	 *            recognized. A rectangle of zero dimension or <code>null</code>
	 *            indicates the whole image.
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(BufferedImage bi, Rectangle rect) throws TesseractException {
		try {
			return doOCR(ImageIOHelper.getIIOImageList(bi), rect);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new TesseractException(e);
		}
	}

	/**
	 * Performs OCR operation.
	 *
	 * @param imageList
	 *            a list of <code>IIOImage</code> objects
	 * @param rect
	 *            the bounding rectangle defines the region of the image to be
	 *            recognized. A rectangle of zero dimension or <code>null</code>
	 *            indicates the whole image.
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(List<IIOImage> imageList, Rectangle rect) throws TesseractException {
		return doOCR(imageList, null, rect);
	}

	/**
	 * Performs OCR operation.
	 *
	 * @param imageList
	 *            a list of <code>IIOImage</code> objects
	 * @param filename
	 *            input file name. Needed only for training and reading a UNLV
	 *            zone file.
	 * @param rect
	 *            the bounding rectangle defines the region of the image to be
	 *            recognized. A rectangle of zero dimension or <code>null</code>
	 *            indicates the whole image.
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(List<IIOImage> imageList, String filename, Rectangle rect) throws TesseractException {

		StringBuilder sb = new StringBuilder();
		int pageNum = 0;

		for (IIOImage oimage : imageList) {
			pageNum++;
			try {
				setImage(oimage.getRenderedImage(), rect);
				sb.append(getOCRText(filename));
			} catch (IOException ioe) {
				// skip the problematic image
				logger.error(ioe.getMessage(), ioe);
			}
		}

		if (renderedFormat == RenderedFormat.HOCR) {
			sb.insert(0, htmlBeginTag).append(htmlEndTag);
		}

		return sb.toString();

	}

	/**
	 * Performs OCR operation. Use <code>SetImage</code>, (optionally)
	 * <code>SetRectangle</code>, and one or more of the <code>Get*Text</code>
	 * functions.
	 *
	 * @param xsize
	 *            width of image
	 * @param ysize
	 *            height of image
	 * @param buf
	 *            pixel data
	 * @param rect
	 *            the bounding rectangle defines the region of the image to be
	 *            recognized. A rectangle of zero dimension or <code>null</code>
	 *            indicates the whole image.
	 * @param bpp
	 *            bits per pixel, represents the bit depth of the image, with 1
	 *            for binary bitmap, 8 for gray, and 24 for color RGB.
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(int xsize, int ysize, ByteBuffer buf, Rectangle rect, int bpp) throws TesseractException {
		return doOCR(xsize, ysize, buf, null, rect, bpp);
	}

	/**
	 * Performs OCR operation. Use <code>SetImage</code>, (optionally)
	 * <code>SetRectangle</code>, and one or more of the <code>Get*Text</code>
	 * functions.
	 *
	 * @param xsize
	 *            width of image
	 * @param ysize
	 *            height of image
	 * @param buf
	 *            pixel data
	 * @param filename
	 *            input file name. Needed only for training and reading a UNLV
	 *            zone file.
	 * @param rect
	 *            the bounding rectangle defines the region of the image to be
	 *            recognized. A rectangle of zero dimension or <code>null</code>
	 *            indicates the whole image.
	 * @param bpp
	 *            bits per pixel, represents the bit depth of the image, with 1
	 *            for binary bitmap, 8 for gray, and 24 for color RGB.
	 * @return the recognized text
	 * @throws TesseractException
	 */
	@Override
	public String doOCR(int xsize, int ysize, ByteBuffer buf, String filename, Rectangle rect, int bpp)
			throws TesseractException {

		try {
			setImage(xsize, ysize, buf, rect, bpp);
			return getOCRText(filename);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new TesseractException(e);
		} finally {
			dispose();
		}
	}

	/**
	 * Initializes Tesseract engine.
	 */
	protected void init() {
		api = TessAPI.INSTANCE;
		handle = api.TessBaseAPICreate();
		StringArray sarray = new StringArray(configList.toArray(new String[0]));
		PointerByReference configs = new PointerByReference();
		configs.setPointer(sarray);
		api.TessBaseAPIInit1(handle, datapath, language, ocrEngineMode, configs, configList.size());
		if (psm > -1) {
			api.TessBaseAPISetPageSegMode(handle, psm);
		}
	}

	/**
	 * Sets Tesseract's internal parameters.
	 */
	protected void setTessVariables() {
		Enumeration<?> em = prop.propertyNames();
		while (em.hasMoreElements()) {
			String key = (String) em.nextElement();
			api.TessBaseAPISetVariable(handle, key, prop.getProperty(key));
		}
	}

	/**
	 * A wrapper for {@link #setImage(int, int, ByteBuffer, Rectangle, int)}.
	 *
	 * @param image
	 *            a rendered image
	 * @param rect
	 *            region of interest
	 * @throws java.io.IOException
	 */
	protected void setImage(RenderedImage image, Rectangle rect) throws IOException {
		setImage(image.getWidth(), image.getHeight(), ImageIOHelper.getImageByteBuffer(image), rect,
				image.getColorModel().getPixelSize());
	}

	protected void setImage(BufferedImage image, Rectangle rect) throws IOException {
		setImage(image.getWidth(), image.getHeight(), ImageIOHelper.convertImageData(image), rect,
				image.getColorModel().getPixelSize());
	}

	/**
	 * Sets image to be processed.
	 *
	 * @param xsize
	 *            width of image
	 * @param ysize
	 *            height of image
	 * @param buf
	 *            pixel data
	 * @param rect
	 *            the bounding rectangle defines the region of the image to be
	 *            recognized. A rectangle of zero dimension or <code>null</code>
	 *            indicates the whole image.
	 * @param bpp
	 *            bits per pixel, represents the bit depth of the image, with 1
	 *            for binary bitmap, 8 for gray, and 24 for color RGB.
	 */
	protected void setImage(int xsize, int ysize, ByteBuffer buf, Rectangle rect, int bpp) {
		int bytespp = bpp / 8;
		int bytespl = (int) Math.ceil(xsize * bpp / 8.0);
		api.TessBaseAPISetImage(handle, buf, xsize, ysize, bytespp, bytespl);

		if (rect != null && !rect.isEmpty()) {
			api.TessBaseAPISetRectangle(handle, rect.x, rect.y, rect.width, rect.height);
		}
	}

	/**
	 * Gets recognized text.
	 *
	 * @param filename
	 *            input file name. Needed only for reading a UNLV zone file.
	 * @param pageNum
	 *            page number; needed for hocr paging.
	 * @return the recognized text
	 * @throws IOException
	 * @throws TesseractException
	 */
	protected String getOCRText(String filename) throws IOException, TesseractException {

		List<Word> words = getSymbols();

		List<Word> layouts = getLayouts();

		Pointer utf8Text = api.TessBaseAPIGetUTF8Text(handle);
		String str = utf8Text.getString(0);
		api.TessDeleteText(utf8Text);
		
		//System.out.println(str);

		dispose();

		StringBuilder temp = new StringBuilder(str);

		if (firstPage) {

			String s = "";

			//int o = 0;

			for (Word word : words) {
				s += word.getText();

				//add

				/*if (word.getBoundingBox().getWidth() > 0) {
				
					File imageFile = new File(filename);
					BufferedImage bi = ImageIO.read(imageFile);
				
					BufferedImage wordImg = cropImage(bi, word.getBoundingBox());
				
					ImageIO.write(wordImg, "png", new File(filename + o++ + ".png"));
				}*/
			}

			// HWR
			boolean numNotFixed = true;
			boolean dayNotFixed = true;
			boolean monthNotFixed = true;
			int check = 0;
			StringBuilder sb = new StringBuilder(s);
			for (int i = 0; i < s.length(); i++) {
				char c = sb.charAt(i);
				if (Character.isDigit(c))
					continue;

				if ((s.substring(0, i).contains("Số") || s.substring(0,i).contains("số")) && numNotFixed) {
					if (c == ':')
						continue;
					if (c == '/') {
						numNotFixed = false;
						continue;
					}
					char reg = doReg(filename, words.get(i).getBoundingBox());
					if (reg != 'a')
						sb.setCharAt(i, reg);
					continue;
				}

				if (s.substring(0, i).contains("ngày") && dayNotFixed) {
					if (c == 't') {
						dayNotFixed = false;
						check = i;
						continue;
					}
					char reg = doReg(filename, words.get(i).getBoundingBox());
					if (reg != 'a')
						sb.setCharAt(i, reg);
					continue;
				}

				if (s.substring(0, i).contains("tháng") && monthNotFixed) {
					if (c == 'n' || s.substring(check, i).contains("20")) {
						monthNotFixed = false;
						continue;
					}
					char reg = doReg(filename, words.get(i).getBoundingBox());
					if (reg != 'a')
						sb.setCharAt(i, reg);
					continue;
				}

				if (s.substring(0, i).contains("năm") || s.substring(0, i).contains("m20")) {
					break;
				}

			}

			s = sb.toString();
			// Merge result

			for (int i = 0, j = 0; i < temp.length() && j < s.length();) {
				char s1 = temp.charAt(i);
				char s2 = s.charAt(j);

				if (s1 == ' ' || s1 == '\n' || s1 == '\t') {
					i++;
					continue;
				}

				if (s2 == ' ' || s2 == '\n' || s2 == '\t') {
					j++;
					continue;
				}

				if (s1 != s2) {
					temp.setCharAt(i, s2);
					System.out.println("Replace '" + s1 + "' with '" + s2 + "'");
				}
				i++;
				j++;

			}

		}

		String result = temp.toString();

		System.out.println("Sign: " + lastPage + " - " + layouts.size());

		if (layouts.size() > 1 && lastPage) {

			int u = 1;
			String sign = "";

			while (sign.length() < 5) {
				Word signature = layouts.get(layouts.size() - u++);

				File imageFile = new File(filename);
				BufferedImage bi = ImageIO.read(imageFile);

				BufferedImage wordImg = cropImage(bi, signature.getBoundingBox());

				BufferedImage outImg = RedRemover.convertRedToWhite(wordImg, 30);

				ImageIO.write(outImg, "png", new File(filename + "sign.png"));

				ITesseract conv = new ConvTesseract();

				sign = conv.doOCR(ImageIO.read(new File(filename + "sign.png")));

				System.out.println("OCR Sign: " + sign);
			}

			result += sign;

		}

		return result;
	}

	private char doReg(String filename, Rectangle rect) {

		if (lenet == null)
			lenet = new Lenet();

		if (rect.getWidth() < 5)
			return 'a';

		try {
			File imageFile = new File(filename);
			BufferedImage bi = ImageIO.read(imageFile);

			BufferedImage wordImg = cropImage(bi, rect);

			return lenet.doHWR(wordImg);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 'a';
	}

	private static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
		BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
		return dest;
	}

	/**
	 * Creates renderers for given formats.
	 *
	 * @param outputbase
	 * @param formats
	 * @return
	 */
	private TessResultRenderer createRenderers(String outputbase, List<RenderedFormat> formats) {
		TessResultRenderer renderer = null;

		for (RenderedFormat format : formats) {
			switch (format) {
			case TEXT:
				if (renderer == null) {
					renderer = api.TessTextRendererCreate(outputbase);
				} else {
					api.TessResultRendererInsert(renderer, api.TessTextRendererCreate(outputbase));
				}
				break;
			case HOCR:
				if (renderer == null) {
					renderer = api.TessHOcrRendererCreate(outputbase);
				} else {
					api.TessResultRendererInsert(renderer, api.TessHOcrRendererCreate(outputbase));
				}
				break;
			case PDF:
				String dataPath = api.TessBaseAPIGetDatapath(handle);
				if (renderer == null) {
					renderer = api.TessPDFRendererCreate(outputbase, dataPath, TRUE);
				} else {
					api.TessResultRendererInsert(renderer, api.TessPDFRendererCreate(outputbase, dataPath, TRUE));
				}
				break;
			case BOX:
				if (renderer == null) {
					renderer = api.TessBoxTextRendererCreate(outputbase);
				} else {
					api.TessResultRendererInsert(renderer, api.TessBoxTextRendererCreate(outputbase));
				}
				break;
			case UNLV:
				if (renderer == null) {
					renderer = api.TessUnlvRendererCreate(outputbase);
				} else {
					api.TessResultRendererInsert(renderer, api.TessUnlvRendererCreate(outputbase));
				}
				break;
			}
		}

		return renderer;
	}

	/**
	 * Creates documents for given renderer.
	 *
	 * @param filename
	 *            input image
	 * @param outputbase
	 *            output filename without extension
	 * @param formats
	 *            types of renderer
	 * @throws TesseractException
	 */
	@Override
	public void createDocuments(String filename, String outputbase, List<RenderedFormat> formats)
			throws TesseractException {
		createDocuments(new String[] { filename }, new String[] { outputbase }, formats);
	}

	/**
	 * Creates documents.
	 *
	 * @param filenames
	 *            array of input files
	 * @param outputbases
	 *            array of output filenames without extension
	 * @param formats
	 *            types of renderer
	 * @throws TesseractException
	 */
	@Override
	public void createDocuments(String[] filenames, String[] outputbases, List<RenderedFormat> formats)
			throws TesseractException {
		if (filenames.length != outputbases.length) {
			throw new RuntimeException("The two arrays must match in length.");
		}

		try {
			for (int i = 0; i < filenames.length; i++) {
				File workingTiffFile = null;
				try {
					String filename = filenames[i];

					// if PDF, convert to multi-page TIFF
					if (filename.toLowerCase().endsWith(".pdf")) {
						workingTiffFile = PdfUtilities.convertPdf2Tiff(new File(filename));
						filename = workingTiffFile.getPath();
					}

					TessResultRenderer renderer = createRenderers(outputbases[i], formats);
					createDocuments(filename, renderer);
					api.TessDeleteResultRenderer(renderer);
				} catch (Exception e) {
					// skip the problematic image file
					logger.error(e.getMessage(), e);
				} finally {
					if (workingTiffFile != null && workingTiffFile.exists()) {
						workingTiffFile.delete();
					}
				}
			}
		} finally {
			dispose();
		}
	}

	/**
	 * Creates documents.
	 *
	 * @param filename
	 *            input file
	 * @param renderer
	 *            renderer
	 * @throws TesseractException
	 */
	private void createDocuments(String filename, TessResultRenderer renderer) throws TesseractException {
		api.TessBaseAPISetInputName(handle, filename); // for reading a UNLV
														// zone file
		int result = api.TessBaseAPIProcessPages(handle, filename, null, 0, renderer);

		if (result == ITessAPI.FALSE) {
			throw new TesseractException("Error during processing page.");
		}
	}

	/**
	 * Gets segmented regions at specified page iterator level.
	 *
	 * @param bi
	 *            input image
	 * @param pageIteratorLevel
	 *            TessPageIteratorLevel enum
	 * @return list of <code>Rectangle</code>
	 * @throws TesseractException
	 */
	@Override
	public List<Rectangle> getSegmentedRegions(BufferedImage bi, int pageIteratorLevel) throws TesseractException {

		try {
			List<Rectangle> list = new ArrayList<Rectangle>();
			setImage(bi, null);

			Boxa boxes = api.TessBaseAPIGetComponentImages(handle, pageIteratorLevel, TRUE, null, null);
			Leptonica leptInstance = Leptonica.INSTANCE;
			int boxCount = leptInstance.boxaGetCount(boxes);
			for (int i = 0; i < boxCount; i++) {
				Box box = leptInstance.boxaGetBox(boxes, i, L_CLONE);
				if (box == null) {
					continue;
				}
				list.add(new Rectangle(box.x, box.y, box.w, box.h));
				PointerByReference pRef = new PointerByReference();
				pRef.setValue(box.getPointer());
				leptInstance.boxDestroy(pRef);
			}

			PointerByReference pRef = new PointerByReference();
			pRef.setValue(boxes.getPointer());
			leptInstance.boxaDestroy(pRef);

			return list;
		} catch (IOException ioe) {
			// skip the problematic image
			logger.error(ioe.getMessage(), ioe);
			throw new TesseractException(ioe);
		} finally {
			dispose();
		}
	}

	/**
	 * Gets recognized words at specified page iterator level.
	 *
	 * @param bi
	 *            input image
	 * @param pageIteratorLevel
	 *            TessPageIteratorLevel enum
	 * @return list of <code>Word</code>
	 */
	@Override
	public List<Word> getWords(BufferedImage bi, int pageIteratorLevel) {

		List<Word> words = new ArrayList<Word>();

		try {
			setImage(bi, null);

			api.TessBaseAPIRecognize(handle, null);
			TessResultIterator ri = api.TessBaseAPIGetIterator(handle);
			TessPageIterator pi = api.TessResultIteratorGetPageIterator(ri);
			api.TessPageIteratorBegin(pi);

			do {
				Pointer ptr = api.TessResultIteratorGetUTF8Text(ri, pageIteratorLevel);
				String text = ptr.getString(0);
				api.TessDeleteText(ptr);
				float confidence = api.TessResultIteratorConfidence(ri, pageIteratorLevel);
				IntBuffer leftB = IntBuffer.allocate(1);
				IntBuffer topB = IntBuffer.allocate(1);
				IntBuffer rightB = IntBuffer.allocate(1);
				IntBuffer bottomB = IntBuffer.allocate(1);
				api.TessPageIteratorBoundingBox(pi, pageIteratorLevel, leftB, topB, rightB, bottomB);
				int left = leftB.get();
				int top = topB.get();
				int right = rightB.get();
				int bottom = bottomB.get();
				Word word = new Word(text, confidence, new Rectangle(left, top, right - left, bottom - top));
				words.add(word);
			} while (api.TessPageIteratorNext(pi, pageIteratorLevel) == TRUE);

			return words;
		} catch (Exception e) {
			return words;
		} finally {
			dispose();
		}
	}

	public List<Word> getSymbols() {

		int level = TessPageIteratorLevel.RIL_SYMBOL;
		List<Word> words = new ArrayList<Word>();

		try {
			api.TessBaseAPIRecognize(handle, null);
			TessResultIterator ri = api.TessBaseAPIGetIterator(handle);
			TessPageIterator pi = api.TessResultIteratorGetPageIterator(ri);
			api.TessPageIteratorBegin(pi);

			do {
				Pointer ptr = api.TessResultIteratorGetUTF8Text(ri, level);
				String text = ptr.getString(0);
				api.TessDeleteText(ptr);
				float confidence = api.TessResultIteratorConfidence(ri, level);
				IntBuffer leftB = IntBuffer.allocate(1);
				IntBuffer topB = IntBuffer.allocate(1);
				IntBuffer rightB = IntBuffer.allocate(1);
				IntBuffer bottomB = IntBuffer.allocate(1);
				api.TessPageIteratorBoundingBox(pi, level, leftB, topB, rightB, bottomB);
				int left = leftB.get();
				int top = topB.get();
				int right = rightB.get();
				int bottom = bottomB.get();
				Word word = new Word(text, confidence, new Rectangle(left, top, right - left, bottom - top));
				words.add(word);
			} while (api.TessPageIteratorNext(pi, level) == TRUE);

			return words;
		} catch (Exception e) {
			return words;
		}
	}

	public List<Word> getLayouts() {

		int level = TessPageIteratorLevel.RIL_BLOCK;
		List<Word> words = new ArrayList<Word>();

		try {
			api.TessBaseAPIRecognize(handle, null);
			TessResultIterator ri = api.TessBaseAPIGetIterator(handle);
			TessPageIterator pi = api.TessResultIteratorGetPageIterator(ri);
			api.TessPageIteratorBegin(pi);

			do {
				Pointer ptr = api.TessResultIteratorGetUTF8Text(ri, level);
				String text = ptr.getString(0);
				api.TessDeleteText(ptr);
				float confidence = api.TessResultIteratorConfidence(ri, level);
				IntBuffer leftB = IntBuffer.allocate(1);
				IntBuffer topB = IntBuffer.allocate(1);
				IntBuffer rightB = IntBuffer.allocate(1);
				IntBuffer bottomB = IntBuffer.allocate(1);
				api.TessPageIteratorBoundingBox(pi, level, leftB, topB, rightB, bottomB);
				int left = leftB.get();
				int top = topB.get();
				int right = rightB.get();
				int bottom = bottomB.get();
				Word word = new Word(text, confidence, new Rectangle(left, top, right - left, bottom - top));
				words.add(word);
			} while (api.TessPageIteratorNext(pi, level) == TRUE);

			return words;
		} catch (Exception e) {
			return words;
		}
	}

	/**
	 * Releases all of the native resources used by this instance.
	 */
	protected void dispose() {
		api.TessBaseAPIDelete(handle);
	}
}
