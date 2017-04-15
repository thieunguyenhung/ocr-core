package etl.cie;

public class LAB {
	public double L;
	public double a;
	public double b;
	public int c = -1;
	public double[] w;
	public double s = -1;

	public LAB(double L, double a, double b) {
		this.L = L;
		this.a = a;
		this.b = b;
	}

	private LAB(double L, double a, double b, int c) {
		this.L = L;
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public String toString() {
		return ((int) L) + "," + ((int) a) + "," + ((int) b);
	}

	public boolean equals(Object o) {
		if (o instanceof LAB) {
			LAB y = (LAB) o;
			return L == y.L && a == y.a && b == y.b;
		} else {
			return false;
		}
	}

	public int hashCode() {
		int x = (int) L;
		int y = (int) (a + 110);
		int z = (int) (b + 110);
		return (x << 16) | (y << 8) | z;
	}

	public LAB copy() {
		LAB x = new LAB(L, a, b, c);
		if (w != null)
			x.w = w.clone();
		return x;
	}

	public double distance(LAB y) {
		double dL = L - y.L, da = a - y.a, db = b - y.b;
		return Math.sqrt(dL * dL + da * da + db * db);
	}

	public int rgb() {
		// first, map CIE L*a*b* to CIE XYZ
		double y = (L + 16) / 116;
		double x = y + a / 500;
		double z = y - b / 200;

		// D65 standard referent
		double X = 0.950470, Y = 1.0, Z = 1.088830;

		x = X * (x > 0.206893034 ? x * x * x : (x - 4.0 / 29) / 7.787037);
		y = Y * (y > 0.206893034 ? y * y * y : (y - 4.0 / 29) / 7.787037);
		z = Z * (z > 0.206893034 ? z * z * z : (z - 4.0 / 29) / 7.787037);

		// second, map CIE XYZ to sRGB
		double r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
		double g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
		double b = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z;
		r = r <= 0.00304 ? 12.92 * r : 1.055 * Math.pow(r, 1 / 2.4) - 0.055;
		g = g <= 0.00304 ? 12.92 * g : 1.055 * Math.pow(g, 1 / 2.4) - 0.055;
		b = b <= 0.00304 ? 12.92 * b : 1.055 * Math.pow(b, 1 / 2.4) - 0.055;

		// third, get sRGB values
		int ir = (int) Math.round(255 * r);
		ir = Math.max(0, Math.min(ir, 255));
		int ig = (int) Math.round(255 * g);
		ig = Math.max(0, Math.min(ig, 255));
		int ib = (int) Math.round(255 * b);
		ib = Math.max(0, Math.min(ib, 255));
		return (0xFF0000 & (ir << 16)) | (0x00FF00 & (ig << 8)) | (0xFF & ib);
	}

	public String hex() {
		int rgb = this.rgb();
		int r = (0xFF & (rgb >> 16));
		int g = (0xFF & (rgb >> 8));
		int b = (0xFF & (rgb));
		String sr = Integer.toHexString(r);
		String sg = Integer.toHexString(g);
		String sb = Integer.toHexString(b);
		if (sr.length() < 2)
			sr = "0" + sr;
		if (sg.length() < 2)
			sg = "0" + sg;
		if (sb.length() < 2)
			sb = "0" + sb;
		return "#" + sr + sg + sb;
	}
}