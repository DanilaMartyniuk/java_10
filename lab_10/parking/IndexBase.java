package parking;

import java.util.Comparator;

public interface IndexBase {
	String[] getKeys(Comparator<String> comp);

	void put(String key, long value);

	boolean contains(String key);

	Long[] get(String key);
}
