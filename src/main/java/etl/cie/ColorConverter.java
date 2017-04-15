package etl.cie;

public class ColorConverter {
	/**
	 * Maps an RGB triple to binned LAB space (D65). Binning is done by
	 * <i>flooring</i> LAB values.
	 * 
	 * @param ri
	 *            Red component of the RGB color.
	 * @param gi
	 *            Green component of the RGB color.
	 * @param bi
	 *            Blue component of the RGB color.
	 * @param binSize
	 *            double input 0 for default.
	 */
	public static LAB fromRGB(int ri, int gi, int bi, double binSize) {
		// first, normalize RGB values
		double r = ri / 255.0;
		double g = gi / 255.0;
		double b = bi / 255.0;

		// D65 standard referent
		double X = 0.950470, Y = 1.0, Z = 1.088830;

		// second, map sRGB to CIE XYZ
		r = r <= 0.04045 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
		g = g <= 0.04045 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
		b = b <= 0.04045 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
		double x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / X, y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / Y, z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / Z;

		// third, map CIE XYZ to CIE L*a*b* and return
		x = x > 0.008856 ? Math.pow(x, 1.0 / 3) : 7.787037 * x + 4.0 / 29;
		y = y > 0.008856 ? Math.pow(y, 1.0 / 3) : 7.787037 * y + 4.0 / 29;
		z = z > 0.008856 ? Math.pow(z, 1.0 / 3) : 7.787037 * z + 4.0 / 29;

		double L = 116 * y - 16;
		double A = 500 * (x - y);
		double B = 200 * (y - z);

		if (binSize > 0) {
			L = binSize * Math.floor(L / binSize);
			A = binSize * Math.floor(A / binSize);
			B = binSize * Math.floor(B / binSize);
		}
		return new LAB(L, A, B);
	}

	/**
	 * Maps an RGB triple to binned LAB space (D65). Binning is done by
	 * <i>rounding</i> LAB values.
	 * 
	 * @param ri
	 *            Red component of the RGB color.
	 * @param gi
	 *            Green component of the RGB color.
	 * @param bi
	 *            Blue component of the RGB color.
	 * @param binSize
	 *            double input 0 for default.
	 */
	public static LAB fromRGBr(int ri, int gi, int bi, double binSize) {
		// first, normalize RGB values
		double r = ri / 255.0;
		double g = gi / 255.0;
		double b = bi / 255.0;

		// D65 standard referent
		double X = 0.950470, Y = 1.0, Z = 1.088830;

		// second, map sRGB to CIE XYZ
		r = r <= 0.04045 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
		g = g <= 0.04045 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
		b = b <= 0.04045 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
		double x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / X, y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / Y, z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / Z;

		// third, map CIE XYZ to CIE L*a*b* and return
		x = x > 0.008856 ? Math.pow(x, 1.0 / 3) : 7.787037 * x + 4.0 / 29;
		y = y > 0.008856 ? Math.pow(y, 1.0 / 3) : 7.787037 * y + 4.0 / 29;
		z = z > 0.008856 ? Math.pow(z, 1.0 / 3) : 7.787037 * z + 4.0 / 29;

		double L = 116 * y - 16;
		double A = 500 * (x - y);
		double B = 200 * (y - z);

		if (binSize > 0) {
			L = binSize * Math.round(L / binSize);
			A = binSize * Math.round(A / binSize);
			B = binSize * Math.round(B / binSize);
		}
		return new LAB(L, A, B);
	}
}
