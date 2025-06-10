package vct.parsers.transform.systemctocol.util;

import java.util.Comparator;

public class StringComparator implements Comparator<Object> {
    @Override
    public int compare(Object a, Object b) {
        return a.toString().compareTo(b.toString());
    }
}