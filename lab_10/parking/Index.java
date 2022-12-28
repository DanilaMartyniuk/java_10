package parking;

import java.io.*;
import java.util.*;
import java.util.zip.*;

class IndexOne2One implements Serializable, IndexBase {
	// Unique keys
	// class release version:
	private static final long serialVersionUID = 1L;

	private TreeMap<String, Long> map;

	public IndexOne2One() {
		map = new TreeMap<String, Long>();
	}

	public String[] getKeys(Comparator<String> comp) {
		String[] result = map.keySet().toArray(new String[0]);
		Arrays.sort(result, comp);
		return result;
	}

	public void put(String key, long value) {
		map.put(key, value);
	}

	public boolean contains(String key) {
		return map.containsKey(key);
	}

	public Long[] get(String key) {
		long pos = map.get(key).longValue();
		return new Long[] { pos };
	}
}

class IndexOne2N implements Serializable, IndexBase {
	// Not unique keys
	// class release version:
	private static final long serialVersionUID = 1L;

	private TreeMap<String, Vector<Long>> map;

	public IndexOne2N() {
		map = new TreeMap<String, Vector<Long>>();
	}

	public String[] getKeys(Comparator<String> comp) {
		String[] result = map.keySet().toArray(new String[0]);
		Arrays.sort(result, comp);
		return result;
	}

	public void put(String key, long value) {
		Vector<Long> arr = map.get(key);
		if (arr == null) {
			arr = new Vector<Long>();
		}
		arr.add(value);
		map.put(key, arr);
	}

	public void put(String keys, // few keys in one string
			String keyDel, // key delimiter
			long value) {
		StringTokenizer st = new StringTokenizer(keys, keyDel);
		int num = st.countTokens();
		for (int i = 0; i < num; i++) {
			String key = st.nextToken();
			key = key.trim();
			put(key, value);
		}
	}

	public boolean contains(String key) {
		return map.containsKey(key);
	}

	public Long[] get(String key) {
		return map.get(key).toArray(new Long[0]);
	}
}

public class Index implements Serializable, Closeable {
	// class release version:
	private static final long serialVersionUID = 1L;

	public IndexOne2One serialNums;
	public IndexOne2N owners;
	public IndexOne2N times;

	public void test(Car car) throws KeyNotUniqueException {
		assert (car != null);
		//check requirements for unique keys
		if (serialNums.contains(car.serialNum)) {
			throw new KeyNotUniqueException(car.serialNum);
		}
	}

	public void put(Car car, long value) throws KeyNotUniqueException {
		test(car);
		serialNums.put(car.serialNum, value);
		owners.put(car.owner, Car.ownerDel, value);

        String time = "from:" + car.startDate + " " + car.startTime + "to" + car.endDate + " " + car.endTime;
		times.put(time, value);
	}

	public Index() {
		serialNums = new IndexOne2One();
		owners = new IndexOne2N();
		times = new IndexOne2N();
	}

	public static Index load(String name) throws IOException,
			ClassNotFoundException {
		Index obj = null;
		try {
			FileInputStream file = new FileInputStream(name);
			try (ZipInputStream zis = new ZipInputStream(file)) {
				ZipEntry zen = zis.getNextEntry();
				if (zen.getName().equals(Buffer.zipEntryName) == false) {
					throw new IOException("Invalid block format");
				}
				try (ObjectInputStream ois = new ObjectInputStream(zis)) {
					obj = (Index) ois.readObject();
				}
			}
		} catch (FileNotFoundException e) {
			obj = new Index();
		}
		if (obj != null) {
			obj.save(name);
		}
		return obj;
	}

	private transient String filename = null;

	public void save(String name) {
		filename = name;
	}

	public void saveAs(String name) throws IOException {
		FileOutputStream file = new FileOutputStream(name);
		try (ZipOutputStream zos = new ZipOutputStream(file)) {
			zos.putNextEntry(new ZipEntry(Buffer.zipEntryName));
			zos.setLevel(ZipOutputStream.DEFLATED);
			try (ObjectOutputStream oos = new ObjectOutputStream(zos)) {
				oos.writeObject(this);
				oos.flush();
				zos.closeEntry();
				zos.flush();
			}
		}
	}

	public void close() throws IOException {
		saveAs(filename);
	}
}
