package etl.core;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class RedRemover {
	private static Color RED_RGB = new Color(255, 0, 0);
	private static Color WHITE_RGB = new Color(255, 255, 255);
	private static Color BLACK_RGB = new Color(0, 0, 0);

	/**
	 * <b>OVERWRITE MODE</b> Remove red color from an image and overwrite
	 * original image
	 * 
	 * @param imagePath
	 *            <i>String</i> image's path
	 * @param threshold
	 *            <i>double</i> threshold of CIEDE2000 distance to remove red
	 *            color, default should be 30
	 */
	public static void removeRed(String imagePath, double threshold) {
		try {
			File input = new File(imagePath);
			BufferedImage image = ImageIO.read(input);
			File ouptut = new File(imagePath);
			ImageIO.write(convertRedToWhite(image, threshold), "png", ouptut);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Remove red color from an image and save result to a new image
	 * 
	 * @param imageInputPath
	 *            <i>String</i> input image's path
	 * @param imageOutputPath
	 *            <i>String</i> output image's path
	 * @param threshold
	 *            <i>double</i> threshold of CIEDE2000 distance to remove red
	 *            color, default should be 30
	 */
	public static void removeRed(String imageInputPath, String imageOutputPath, double threshold) {
		try {
			File input = new File(imageInputPath);
			BufferedImage image = ImageIO.read(input);
			File ouptut = new File(imageOutputPath);
			ImageIO.write(convertRedToWhite(image, threshold), "png", ouptut);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Replace red pixel with white pixel in an image
	 * 
	 * @param img
	 *            <i>BufferedImage</i> image to replace
	 * @param threshold
	 *            <i>double</i> threshold of CIEDE2000 distance to remove red
	 *            color, default should be 30
	 */
	public static BufferedImage convertRedToWhite(BufferedImage img, double threshold) {
		for (int x = 0; x < img.getWidth(); ++x)
			for (int y = 0; y < img.getHeight(); ++y) {
				Color c = new Color(img.getRGB(x, y));
				if (ColorDistance.getCIEDE2000Distance(c, RED_RGB) <= 50
						&& ColorDistance.getCIEDE2000Distance(c, BLACK_RGB) >= 30
						) {
					img.setRGB(x, y, WHITE_RGB.getRGB());
				} else {
					img.setRGB(x, y, c.getRGB());
				}
			}
		return img;
	}

	public static void getHexColor(String filePath) {
		try {
			File input = new File(filePath);
			BufferedImage image = ImageIO.read(input);
			PrintWriter writer = new PrintWriter(new File("list.txt"));
			writer.write("<table id='myTable' border='0' width='100%'><tbody>");
			ArrayList<String> lstColor = new ArrayList<>();
			for (int x = 0; x < image.getWidth(); ++x)
				for (int y = 0; y < image.getHeight(); ++y) {
					Color c = new Color(image.getRGB(x, y));

					String hex = String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());

					if (!lstColor.contains(hex)) {
						writer.write("<tr>");
						writer.write("<td width='65%' bgcolor='" + hex + "'></td>");
						writer.write("<td width='25%' bgcolor='#FFFFFF'>" + hex + "</td>");
						writer.write("<td><input type='button' value='Delete' onclick='deleteRow(this)'></td>");
						writer.write("</tr>");
						lstColor.add(hex);
					}
				}
			writer.write("</tbody></table>");
			writer.write("<script>function deleteRow(r){var i = r.parentNode.parentNode.rowIndex;document.getElementById('myTable').deleteRow(i);}</script>");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		RedRemover.removeRed("5.png", "5-out.png", 30);
		System.out.println("Done");
		// getHexColor("/Users/hungthieu/Documents/eclipseWS/RedColorRemover/cc.jpg");
		// System.out.println(calculateColorSimilarity("FFF6EF", "FF0000"));
	}
}
