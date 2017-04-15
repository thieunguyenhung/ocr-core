
import java.io.File;
import java.io.PrintWriter;

import etl.*;

public class Test {
	public static void writeFile(String text, String fileName) {
		try {
			PrintWriter writer = new PrintWriter("documents/output/" + fileName + ".txt", "UTF-8");
			writer.println(text);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ETL etl = new ETL();
		for (int i = 13; i <= 13; i++) {
			writeFile(etl.processing(new File("documents/pdf/" + i + ".pdf")), String.valueOf(i));
			System.out.println("Done " + i);
		}
	}
}
