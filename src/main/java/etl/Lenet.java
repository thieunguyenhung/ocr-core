package etl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class Lenet {

	private MultiLayerNetwork model;
	private final String SYSTEM_PATH = System.getProperty("user.dir") + "/tessdata/";
	private final String MODEL = "Mnist.zip";

	public Lenet() {
		try {
			if (model == null) {
				File location = new File(SYSTEM_PATH + MODEL);
				model = ModelSerializer.restoreMultiLayerNetwork(location);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public char doHWR(BufferedImage buff) throws IOException {
		INDArray arr = getImgInTensor(buff);

		printImg(arr);

		INDArray output = model.output(arr, false);

		return getOutput(output);
	}

	public char getOutput(INDArray output) {
		int result = 0;

		for (int i = 0; i < output.length(); i++) {
			if (output.getFloat(i) > output.getFloat(result)) {
				result = i;
			}
		}

		System.out.println("Recognite " + result + " with confidence=" + output.getFloat(result));
		//if (output.getFloat(result) >= 0.5)
		return Character.forDigit(result, 10);
		//else return 'a';
	}

	public INDArray getImgInTensor(BufferedImage in) throws IOException {

		in = invertImage(in);
		in = scale(in, 28, 28);

		Raster ras = in.getData();
		float[] featureVec = new float[ras.getWidth() * ras.getHeight()];
		ras.getPixels(0, 0, ras.getWidth(), ras.getHeight(), featureVec);

		for (int j = 0; j < featureVec.length; j++) {
			/*if (featureVec[j] > 30.0f)
				featureVec[j] = 1.0f;
			else
				featureVec[j] = 0.0f;*/
			featureVec[j] = featureVec[j] / 255.0f;
		}

		INDArray nd = Nd4j.create(featureVec);
		return nd;
	}

	public void printImg(INDArray output) {
		for (int i = 0; i < output.columns(); i++) {
			if (output.getFloat(i) > 0.12)
				System.out.print(1 + ",");
			else
				System.out.print(0 + ",");
			if ((i + 1) % 28 == 0)
				System.out.println();
		}
	}

	public BufferedImage scale(BufferedImage imageToScale, int dWidth, int dHeight) {
		BufferedImage scaledImage = null;
		if (imageToScale != null) {
			scaledImage = new BufferedImage(dWidth, dHeight, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D graphics2D = scaledImage.createGraphics();

			int x = dWidth / 2 - imageToScale.getWidth() / 2;
			int y = dHeight / 2 - imageToScale.getHeight() / 2;

			graphics2D.drawImage(imageToScale, x, y, imageToScale.getWidth(), imageToScale.getHeight(), null);
			graphics2D.dispose();
		}
		return scaledImage;
	}

	public BufferedImage invertImage(BufferedImage inputFile) {

		for (int x = 0; x < inputFile.getWidth(); x++) {
			for (int y = 0; y < inputFile.getHeight(); y++) {
				int rgba = inputFile.getRGB(x, y);
				Color col = new Color(rgba, true);
				col = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
				inputFile.setRGB(x, y, col.getRGB());
			}
		}
		return inputFile;
	}
}
