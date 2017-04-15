package etl.core;

import java.awt.Color;

import etl.cie.ColorConverter;
import etl.cie.LAB;

public class ColorDistance {
	private static double calculateCIEDE2000(LAB x, LAB y) {
		// adapted from Sharma et al's MATLAB implementation at
		// http://www.ece.rochester.edu/~gsharma/ciede2000/

		// parametric factors, use defaults
		double kl = 1, kc = 1, kh = 1;

		// compute terms
		double pi = Math.PI, L1 = x.L, a1 = x.a, b1 = x.b, Cab1 = Math.sqrt(a1 * a1 + b1 * b1), L2 = y.L, a2 = y.a, b2 = y.b, Cab2 = Math.sqrt(a2 * a2 + b2 * b2), Cab = 0.5 * (Cab1 + Cab2), G = 0.5 * (1 - Math.sqrt(Math.pow(Cab, 7) / (Math.pow(Cab, 7) + Math.pow(25, 7)))), ap1 = (1 + G) * a1, ap2 = (1 + G) * a2, Cp1 = Math.sqrt(ap1 * ap1 + b1 * b1), Cp2 = Math.sqrt(ap2 * ap2 + b2 * b2), Cpp = Cp1 * Cp2;

		// ensure hue is between 0 and 2pi
		double hp1 = Math.atan2(b1, ap1);
		if (hp1 < 0)
			hp1 += 2 * pi;
		double hp2 = Math.atan2(b2, ap2);
		if (hp2 < 0)
			hp2 += 2 * pi;

		double dL = L2 - L1, dC = Cp2 - Cp1, dhp = hp2 - hp1;

		if (dhp > +pi)
			dhp -= 2 * pi;
		if (dhp < -pi)
			dhp += 2 * pi;
		if (Cpp == 0)
			dhp = 0;

		// Note that the defining equations actually need
		// signed Hue and chroma differences which is different
		// from prior color difference formulae
		double dH = 2 * Math.sqrt(Cpp) * Math.sin(dhp / 2);

		// Weighting functions
		double Lp = 0.5 * (L1 + L2), Cp = 0.5 * (Cp1 + Cp2);

		// Average Hue Computation
		// This is equivalent to that in the paper but simpler programmatically.
		// Average hue is computed in radians and converted to degrees where
		// needed
		double hp = 0.5 * (hp1 + hp2);
		// Identify positions for which abs hue diff exceeds 180 degrees
		if (Math.abs(hp1 - hp2) > pi)
			hp -= pi;
		if (hp < 0)
			hp += 2 * pi;

		// Check if one of the chroma values is zero, in which case set
		// mean hue to the sum which is equivalent to other value
		if (Cpp == 0)
			hp = hp1 + hp2;

		double Lpm502 = (Lp - 50) * (Lp - 50), Sl = 1 + 0.015 * Lpm502 / Math.sqrt(20 + Lpm502), Sc = 1 + 0.045 * Cp, T = 1 - 0.17 * Math.cos(hp - pi / 6) + 0.24 * Math.cos(2 * hp) + 0.32 * Math.cos(3 * hp + pi / 30) - 0.20 * Math.cos(4 * hp - 63 * pi / 180), Sh = 1 + 0.015 * Cp * T, ex = (180 / pi * hp - 275) / 25, delthetarad = (30 * pi / 180) * Math.exp(-1 * (ex * ex)), Rc = 2 * Math.sqrt(Math.pow(Cp, 7) / (Math.pow(Cp, 7) + Math.pow(25, 7))), RT = -1 * Math.sin(2 * delthetarad) * Rc;

		dL = dL / (kl * Sl);
		dC = dC / (kc * Sc);
		dH = dH / (kh * Sh);

		// The CIE 00 color difference
		return Math.sqrt(dL * dL + dC * dC + dH * dH + RT * dC * dH);
	}

	public static double getCIEDE2000Distance(Color color1, Color color2) {
		LAB lab1 = ColorConverter.fromRGB(color1.getRed(), color1.getGreen(), color1.getBlue(), 0);
		LAB lab2 = ColorConverter.fromRGB(color2.getRed(), color2.getGreen(), color2.getBlue(), 0);
		return calculateCIEDE2000(lab1, lab2);
	}

	public static double getEuclideanDistance(Color rgb1, Color rgb2) {
		return Math.sqrt(Math.pow(rgb1.getRed() - rgb2.getRed(), 2) + Math.pow(rgb1.getGreen() - rgb2.getGreen(), 2) + Math.pow(rgb1.getBlue() - rgb2.getBlue(), 2));
	}

	public static void main(String[] args) {
		Color c1 = new Color(255, 0, 0);
		Color c2 = new Color(245, 0, 0);
		System.out.println(ColorDistance.getCIEDE2000Distance(c1, c2));
		System.out.println(ColorDistance.getEuclideanDistance(c1, c2));
	}
}