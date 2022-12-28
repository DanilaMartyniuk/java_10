package parking;

import java.util.Comparator;

public class KeyComp implements Comparator<String> {
	public int compare(String o1, String o2) {
		// right order:
		return o1.compareTo(o2);
	}
}
