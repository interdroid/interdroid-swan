package interdroid.swan.crossdevice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Converter {

	/**
	 * Writes an object to a String.
	 */
	public static final String objectToString(Object o) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(o);
		oos.close();
		oos = null;
		return URLEncoder.encode(new String(bos.toByteArray(), "ISO_8859-1"),
				"ISO_8859-1");
	}

	/**
	 * Reads an object from String.
	 */
	public static final Object stringToObject(String s) throws IOException,
			ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(URLDecoder.decode(
				s, "ISO_8859-1").getBytes("ISO_8859-1"));
		ObjectInputStream ois = new ObjectInputStream(bis);
		Object o = ois.readObject();
		ois.close();
		return o;
	}

}
