package com._4point.aem.formsfeeder.core.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Utilities class to help smooth the transition back to JDK 8 from JDK 11.
 *
 */
public class Jdk8Utils {
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	/**
	 * Used to replace the JDK11 InputStream.readAllBytes() routine.
	 * 
	 * @param is
	 * @return
	 */
	public static byte[] readAllBytes(InputStream is) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
        transfer(is, out);
        return out.toByteArray();
	}

	/**
	 * Replaces InputStream transferTo()
	 * 
	 * @param is
	 * @param out
	 * @throws IOException
	 */
	public static void transfer(InputStream is, OutputStream out) throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = is.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
        }
	}

	/**
	 * Replaces String.isBlank()
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isBlank(String s) {
		return s.trim().isEmpty();
	}
	
	/**
	 * Replaces Map.copyOf()
	 * 
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @return
	 */
	public static <K,V> Map<K,V> copyOfMap(Map<? extends K, ? extends V> map) {
		return Collections.unmodifiableMap(new HashMap<>(map));
	}

	/**
	 * Replaces Map.ofEntries()
	 * 
	 * @param <E>
	 * @param es
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> mapOfEntries(Map.Entry<? extends K, ? extends V>... entries) {
		Map<K, V> map = new HashMap<K,V>();
		for (Entry<? extends K, ? extends V> entry : entries) {
			map.put((K) entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(map);
	}
	
	/**
	 * Replaces Map.of() for Strings
	 * 
	 * @param <K>
	 * @param <V>
	 * @param es
	 * @return
	 */
	public static Map<String,String> mapOf(String... es) {
		if (es.length % 2 != 0) {
			throw new IllegalArgumentException("mapOf can only handle an even number of arguments! Invalid number of arguments (" + es.length + ").");
		}
		Map<String, String> map = new HashMap<>();
		int numEntries = es.length / 2;
		for (int i = 0; i < numEntries; i++) {
			map.put(es[i * 2], es[(i * 2) + 1]);
		}
		
		return Collections.unmodifiableMap(map);
	}	
	/**
	 * Replaces Set.copyOf()
	 * 
	 * @param <E>
	 * @param coll
	 * @return
	 */
	public static <E> Set<E> copyOfSet(Collection<? extends E> coll) {
		return Collections.unmodifiableSet(new HashSet<>(coll));
	}

	/**
	 * Replaces Set.of()
	 * 
	 * @param <E>
	 * @param es
	 * @return
	 */
	@SafeVarargs
	public static <E> Set<E> setOf(E... es) {
		return Collections.unmodifiableSet(new HashSet<E>(Arrays.asList(es))
				);
	}
	
	/**
	 * Replaces List.copyOf()
	 * 
	 * @param <E>
	 * @param es
	 * @return
	 */
	public static <E> List<E> copyOfList(Collection<? extends E> coll) {
		return Collections.unmodifiableList(new ArrayList<>(coll));
	}

	/**
	 * Replaces List.of()
	 * 
	 * @param <E>
	 * @param es
	 * @return
	 */
	@SafeVarargs
	public static <E> List<E> listOf(E... es) {
		return Collections.unmodifiableList(Arrays.asList(es));
	}

	/**
	 * Replaces Collections.toUnmodifiableMap()
	 * 
	 * @param <T>
	 * @param <K>
	 * @param <U>
	 * @param keyMapper
	 * @param valueMapper
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T, K, U> Collector<T, ?, Map<K,U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
	        Objects.requireNonNull(keyMapper, "keyMapper");
	        Objects.requireNonNull(valueMapper, "valueMapper");
	        return Collectors.collectingAndThen(
	                Collectors.toMap(keyMapper, valueMapper),
	                map -> (Map<K,U>)mapOfEntries(map.entrySet().toArray(new Map.Entry[0])));
	    }
}
