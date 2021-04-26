package com._4point.aem.formsfeeder.core.datasource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListDecoder;
import com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListEncoder;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

/**
 * Wraps a list of DataSource objects and provides common functions for operating on that list.
 * 
 * In most cases, client code will not directly interact with a DataSourceList (other than to determine whether it's empty)
 * but, instead, will get a builder (to create a DataSourceList) or a Deconstructor (to extract data from a DataSourceList). 
 *
 */
public class DataSourceList implements Iterable<DataSource> {
	private static final DataSourceList EMPTY_LIST = new DataSourceList();
	
	private final List<DataSource> list;

	/**
	 * Constructs an empty list.  Just used by the EMPTY_LIST constant.
	 */
	private DataSourceList() {
		this.list =  Collections.emptyList();
	}
	
	/**
	 * Private constructor that is used by from() method.  It makes a defensive copy of the provided list. 
	 * 
	 * @param list
	 */
	private DataSourceList(List<DataSource> list) {
		this.list = Jdk8Utils.copyOfList(list);
	}

	/**
	 * Predicate to select DataSource by name.
	 * 
	 * @param name
	 * @return
	 */
	public static Predicate<DataSource> byName(String name) {
		return (ds)->ds.name().equals(Objects.requireNonNull(name, "Target DataSource name cannot be null."));
	}

	/**
	 * Returns the unmodifiable list of DataSource objects.
	 * 
	 * @return
	 */
	public final List<DataSource> list() {
		return Collections.unmodifiableList(list);
	}

	/**
	 * Returns the number of items in the DataSourceList.
	 * 
	 * @return
	 */
	public final int size() {
		return list.size();
	}

	/**
	 * Gets the first DataSource with the specified name.
	 * 
	 * Will generate a NullPointerException if the name parameter is null.
	 * 
	 * Shortcut function for <code>getDataSource((ds)-&gt;ds.name().equals(name))</code>
	 * 
	 * @param name
	 * @return the first DataSource with the specified name
	 */
	public final Optional<DataSource> getDataSourceByName(final String name) {
		return getDataSource(byName(name));
	}

	/**
	 * Gets the first DataSource which where the predicate provided evaluates to true.
	 * 
	 * Will generate a NullPointerException if the predicate parameter is null.
	 * 
	 * @param predicate
	 * @return the first DataSource which where the predicate provided evaluates to true
	 */
	public final Optional<DataSource> getDataSource(final Predicate<DataSource> predicate) {
		Objects.requireNonNull(predicate, "Predicate provided cannot be null.");
		for (DataSource ds : list) {
			if (predicate.test(ds)) {
				return Optional.of(ds);
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns a list of the DataSource objects with the specified name.
	 * 
	 * Will generate a NullPointerException if the name parameter is null.
	 * 
	 * Shortcut function for <code>getDataSources((ds)-&gt;ds.name().equals(name))</code>
	 * 
	 * @param name
	 * @return a list of the DataSource objects with the specified name
	 */
	public final List<DataSource> getDataSourcesByName(String name) {
		return getDataSources(byName(name));
	}

	/**
	 * Gets a list of DataSource objects where the predicate provided evaluates to true.
	 * 
	 * Will generate a NullPointerException if the predicate parameter is null.
	 * 
	 * @param predicate
	 * @return a list of DataSource objects where the predicate provided evaluates to true
	 */
	public final List<DataSource> getDataSources(final Predicate<DataSource> predicate) {
		Objects.requireNonNull(predicate, "Predicate provided cannot be null.");
		ArrayList<DataSource> found = new ArrayList<>();
		for (DataSource ds : list) {
			if (predicate.test(ds)) {
				found.add(ds);
			}
		}
		return Jdk8Utils.copyOfList(found);
	}

	/**
	 * Static constructor for DataSourceList.  A defensive copy is made of the list, so that subsequent changes
	 * do not affect the list.
	 * 
	 * @param list
	 * @return a DataSourceList containing the DataSources in the list provided
	 */
	public static DataSourceList from(List<DataSource> list) {
		if (list.isEmpty()) {
			return EMPTY_LIST;	// Don't bother creating a new object.  Re-use EMPTY_LIST.
		} else {
			return new DataSourceList(list);
		}
	}
	
	/**
	 * Static constructor for DataSourceList.  Merges one or more other DataSourceLists into one. 
	 * 
	 * @param srcLists
	 * @return a DataSourceList containing all the DataSources in the DataSourceLists provided
	 */
	public static DataSourceList from(DataSourceList... srcLists) {
		List<DataSource> accumulator = new ArrayList<>();
		for(DataSourceList srcList : srcLists) {
			accumulator.addAll(srcList.list());
		}
		return new DataSourceList(accumulator);
	}
	/**
	 * Static constructor for an empty DataSourceList.
	 * 
	 * @return an empty DataSourceList
	 */
	public static DataSourceList emptyList() {
		return EMPTY_LIST;
	}
	
	/**
	 * Returns true if the list is empty.
	 * 
	 * @see List.isEmpty()
	 * 
	 * @return true if list is empty otherwise false
	 */
	public final boolean isEmpty() {
		return this.list().isEmpty();
	}
	
	/**
	 * Performs the given action for each element of the Iterable until all elements have been processed or the action throws an exception.
	 * 
	 * @see Iterable.forEach()
	 * 
	 * @param action
	 */
	public void forEach(Consumer<? super DataSource> action) {
		this.list.forEach(action);
	}

	/**
	 * Returns an iterator over elements of type DataSource.
	 * 
	 * @see Iterable.iterator()
	 * 
	 * @return
	 */
	public Iterator<DataSource> iterator() {
		return this.list.iterator();
	}

	/**
	 * Returns the element at the specified position in this list.
	 * 
	 * @see List.get()
	 * 
	 * @param index
	 * @return
	 */
	public DataSource get(int index) {
		return this.list.get(index);
	}

	/**
	 * Creates a Spliterator over the elements described by this Iterable.
	 * 
	 * @see Iterable.spliterator()
	 * 
	 * @return
	 */
	public Spliterator<DataSource> spliterator() {
		return this.list.spliterator();
	}

	/**
	 * Returns a sequential Stream with this collection as its source.
	 * 
	 * @see Collection.stream()
	 * 
	 * @return
	 */
	public Stream<DataSource> stream() {
		return this.list.stream();
	}

	/**
	 * Returns a possibly parallel Stream with this collection as its source.
	 * 
	 * @see List.parallelStream()
	 * 
	 * @return
	 */
	public Stream<DataSource> parallelStream() {
		return this.list.parallelStream();
	}

	/**
	 * Create a DataSourceList.Builder for building a DataSourceList.
	 * 
	 * @return new DataSourceList.Builder object
	 */
	public static Builder builder() {
		return Builder.newBuilder();
	}
	
	/**
	 * Create a DataSourceList.Builder for building a DataSourceList.
	 * 
	 * @return new DataSourceList.Builder object
	 */
	public static Builder builder(DataSourceList dsl) {
		return Builder.newBuilder(dsl);
	}
	
	/**
	 * Create a DataSourceList using a function that accepts a DataSourceList.Builder, (presumably) adds items to the DataSourceList via that builder and returns a DataSourceList.Builder
	 * 
	 * This function is a convenience function that allows an object to add its properties into a DataSourceList using a DataSourceList.Builder.
	 * This is handy for serializing objects.  The object can define a ${code Function<DataSourceList.Builder, DataSourceList.Builder>} that
	 * adds itself to a DataSourceList.  This function can then be called to generate a DataSourceList using that function.
	 * 
	 * It performs the following: ${code fieldBuilder.apply(DataSourceList.builder()).build()}
	 * 
	 * @param fieldBuilder function that adds to the data source list via the supplied builder and returns a builder.
	 * @return
	 */
	public static DataSourceList build(Function<DataSourceList.Builder, DataSourceList.Builder> fieldBuilder) {
		return fieldBuilder.apply(DataSourceList.builder()).build();
	}
	
	/**
	 * DataSourceList.Deconstructor for pulling apart (i.e. deconstructing) a DataSourceList.
	 * 
	 * @return new DataSourceList.Deconstructor object
	 */
	public Deconstructor deconstructor() {
		return Deconstructor.from(this);
	}
	
	public static class Builder {

		List<DataSource> underConstruction = new ArrayList<>();
		
		private Builder() {
		}
		
		private static Builder newBuilder() {
			return new Builder();
		}

		private static Builder newBuilder(DataSourceList dsl) {
			final Builder builder = new Builder();
			for (DataSource ds : dsl.list()) {	// transfer data from seed Dsl into new builder
				builder.add(ds);
			}
			return builder;
		}

		public DataSourceList build() {
			return DataSourceList.from(underConstruction);
		}

		/**
		 * Convert contents of a DataSource to a String.  Assumes that the source input stream is UTF-8.
		 * 
		 * @param ds
		 * @return contents of the DataSource as a String object.
		 */
		public static final <T> DataSource objToStringDS(String name, T object) {
			return new StringDataSource(Objects.toString(object), Objects.requireNonNull(name, "Name cannot be null.")); 
		}

		public Builder add(DataSource ds) {
			underConstruction.add(Objects.requireNonNull(ds, "DataSource cannot be null."));
			return this;
		}
		
		public Builder add(String name, String s) {
			underConstruction.add(new StringDataSource(s, Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}
		
		public Builder add(String name, Path p) {
			underConstruction.add(new FileDataSource(p, Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}

		public Builder add(String name, byte[] ba) {
			underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}

		public Builder add(String name, byte[] ba, MimeType contentType) {
			underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), contentType));
			return this;
		}

		public Builder add(String name, byte[] ba, Path p) {
			ByteArrayDataSource ds = new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."));
			ds.filename(p);
			underConstruction.add(ds);
			return this;
		}

		public Builder add(String name, byte[] ba, MimeType contentType, Path p) {
			ByteArrayDataSource ds = new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), contentType);
			ds.filename(p);
			underConstruction.add(ds);
			return this;
		}

		public Builder add(String name, int i) {
			underConstruction.add(new StringDataSource(Integer.toString(i), Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}

		public Builder add(String name, boolean b) {
			underConstruction.add(new StringDataSource(Boolean.toString(b), Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}

		public Builder add(String name, float f) {
			underConstruction.add(new StringDataSource(Float.toString(f), Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}

		public Builder add(String name, double d) {
			underConstruction.add(new StringDataSource(Double.toString(d), Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}

		public Builder add(String name, long l) {
			underConstruction.add(new StringDataSource(Long.toString(l), Objects.requireNonNull(name, "Name cannot be null.")));
			return this;
		}

		public Builder add(String name, DataSourceList dsl) {
			underConstruction.add(new ByteArrayDataSource(dataSourceListToByteArray(dsl), Objects.requireNonNull(name, "Name cannot be null."), XmlDataSourceListEncoder.DSL_MIME_TYPE));
			return this;
		}

		public Builder add(String name, String s, Map<String, String> attributes) {
			underConstruction.add(new StringDataSource(s, Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}
		
		public Builder add(String name, Path p, Map<String, String> attributes) {
			underConstruction.add(new FileDataSource(p, Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}

		public Builder add(String name, byte[] ba, Map<String, String> attributes) {
			underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}

		public Builder add(String name, byte[] ba, MimeType contentType, Map<String, String> attributes) {
			underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), contentType, attributes));
			return this;
		}

		public Builder add(String name, byte[] ba, Path p, Map<String, String> attributes) {
			ByteArrayDataSource ds = new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), attributes);
			ds.filename(p);
			underConstruction.add(ds);
			return this;
		}

		public Builder add(String name, byte[] ba, MimeType contentType, Path p, Map<String, String> attributes) {
			ByteArrayDataSource ds = new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), contentType, attributes);
			ds.filename(p);
			underConstruction.add(ds);
			return this;
		}

		public Builder add(String name, int i, Map<String, String> attributes) {
			underConstruction.add(new StringDataSource(Integer.toString(i), Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}

		public Builder add(String name, boolean b, Map<String, String> attributes) {
			underConstruction.add(new StringDataSource(Boolean.toString(b), Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}

		public Builder add(String name, float f, Map<String, String> attributes) {
			underConstruction.add(new StringDataSource(Float.toString(f), Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}

		public Builder add(String name, double d, Map<String, String> attributes) {
			underConstruction.add(new StringDataSource(Double.toString(d), Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}

		public Builder add(String name, long l, Map<String, String> attributes) {
			underConstruction.add(new StringDataSource(Long.toString(l), Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
		}

		public Builder add(String name, DataSourceList dsl, Map<String, String> attributes) {
			underConstruction.add(new ByteArrayDataSource(dataSourceListToByteArray(dsl), Objects.requireNonNull(name, "Name cannot be null."), XmlDataSourceListEncoder.DSL_MIME_TYPE, attributes));
			return this;
		}

		public Builder addDataSourceList(String name, Function<DataSourceList.Builder, DataSourceList.Builder> fieldBuilder) {
			return this.add(name, DataSourceList.build(fieldBuilder));
		}

		public Builder addDataSources(List<DataSource> dsList) {
			dsList.forEach(ds->underConstruction.add(ds));
			return this;
		}
		
		public Builder addStrings(String name, List<String> sList) {
			sList.forEach(s->underConstruction.add(new StringDataSource(s, Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}
		
		public Builder addPaths(String name, List<Path> pList) {
			pList.forEach(p->underConstruction.add(new FileDataSource(p, Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}

		public Builder addByteArrays(String name, List<byte[]> baList) {
			baList.forEach(ba->underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}

		public Builder addByteArrays(String name, List<byte[]> baList, MimeType contentType) {
			baList.forEach(ba->underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), contentType)));
			return this;
		}

		public Builder addIntegers(String name, List<Integer> iList) {
			iList.forEach(i->underConstruction.add(new StringDataSource(i.toString(), Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}

		public Builder addBooleans(String name, List<Boolean> bList) {
			bList.forEach(b->underConstruction.add(new StringDataSource(b.toString(), Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}

		public Builder addFloats(String name, List<Float> fList) {
			fList.forEach(f->underConstruction.add(new StringDataSource(f.toString(), Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}

		public Builder addDoubles(String name, List<Double> dList) {
			dList.forEach(d->underConstruction.add(new StringDataSource(d.toString(), Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}

		public Builder addLongs(String name, List<Long> lList) {
			lList.forEach(l->underConstruction.add(new StringDataSource(l.toString(), Objects.requireNonNull(name, "Name cannot be null."))));
			return this;
		}

		public Builder addDataSourceLists(String name, List<DataSourceList> lList) {
			lList.forEach(l->underConstruction.add(new ByteArrayDataSource(dataSourceListToByteArray(l), Objects.requireNonNull(name, "Name cannot be null."), XmlDataSourceListEncoder.DSL_MIME_TYPE)));
			return this;
		}

		public Builder addStrings(String name, List<String> sList, Map<String, String> attributes) {
			sList.forEach(s->underConstruction.add(new StringDataSource(s, Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}
		
		public Builder addPaths(String name, List<Path> pList, Map<String, String> attributes) {
			pList.forEach(p->underConstruction.add(new FileDataSource(p, Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}

		public Builder addByteArrays(String name, List<byte[]> baList, Map<String, String> attributes) {
			baList.forEach(ba->underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}

		public Builder addByteArrays(String name, List<byte[]> baList, MimeType contentType, Map<String, String> attributes) {
			baList.forEach(ba->underConstruction.add(new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), contentType, attributes)));
			return this;
		}

		public Builder addIntegers(String name, List<Integer> iList, Map<String, String> attributes) {
			iList.forEach(i->underConstruction.add(new StringDataSource(i.toString(), Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}

		public Builder addBooleans(String name, List<Boolean> bList, Map<String, String> attributes) {
			bList.forEach(b->underConstruction.add(new StringDataSource(b.toString(), Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}

		public Builder addFloats(String name, List<Float> fList, Map<String, String> attributes) {
			fList.forEach(f->underConstruction.add(new StringDataSource(f.toString(), Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}

		public Builder addDoubles(String name, List<Double> dList, Map<String, String> attributes) {
			dList.forEach(d->underConstruction.add(new StringDataSource(d.toString(), Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}

		public Builder addLongs(String name, List<Long> lList, Map<String, String> attributes) {
			lList.forEach(l->underConstruction.add(new StringDataSource(l.toString(), Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
		}
		
		public Builder addDataSourceLists(String name, List<DataSourceList> lList, Map<String, String> attributes) {
			lList.forEach(l->underConstruction.add(new ByteArrayDataSource(dataSourceListToByteArray(l), Objects.requireNonNull(name, "Name cannot be null."), XmlDataSourceListEncoder.DSL_MIME_TYPE, attributes)));
			return this;
		}
		
		private static byte[] dataSourceListToByteArray(DataSourceList dsl) {
			ByteArrayOutputStream contents = new ByteArrayOutputStream();
			try (XmlDataSourceListEncoder encoder = XmlDataSourceListEncoder.wrap(contents)) {
				encoder.encode(dsl);
			} catch (IOException | XMLStreamException | FactoryConfigurationError e) {
				// This should never happen.
				String msg = e.getMessage();
				throw new IllegalStateException("Error while encoding DataSourceList (" + (msg != null ? msg : "null") + ").", e);
			}
			return contents.toByteArray();
		}
		
		// Default registry is a immutable registry containing all the standard deconstructor functions.
		private BuilderFunctionRegistry defaultRegistry = BuilderFunctionRegistry.from(StandardMappers.MAPPER_LIST.stream().collect(Collectors.toMap(Mapper::target, Mapper::to)));
		// Normal registry contains any deconstructor functions registered by the user.
		private BuilderFunctionRegistry registry = BuilderFunctionRegistry.create();
		
		private final <T> BiFunction<String, ? super T, DataSource> registryLookup(Class<? super T> dest) {
			BiFunction<String, ? super T, DataSource> function = registry.get(dest);
			if (function != null) {
				return function;
			} else {
				BiFunction<String, ? super T, DataSource> defaultFunction = defaultRegistry.get(dest);
				if (defaultFunction != null) {
					return defaultFunction;
				} else {
					throw new UnsupportedOperationException("No mapping function found for class '" + dest.getName() + "'" );
				}
			}
		}
		
		public final <T> Builder register(Class<? extends T> type, BiFunction<String, ? extends T, DataSource> mapper) {
			registry.put(type, mapper);
			return this;
		}
		public final <T> Builder addObject(String name, T object, Class<? super T> objectClass) {
			underConstruction.add(registryLookup(objectClass).apply(Objects.requireNonNull(name, "Name cannot be null."), object));
			return this;
		}
		
		public final <T> Builder addObjects(String name, List<T> lList, Class<? super T> objectClass) {
			BiFunction<String, ? super T, DataSource> biFunction = registryLookup(objectClass);
			Objects.requireNonNull(name, "Name cannot be null.");
			lList.forEach(l->underConstruction.add(biFunction.apply(name, l)));
			return this;
		}
		
		private static class BuilderFunctionRegistry {
			private Map<Class<?>, BiFunction<String, ?, DataSource>> mappingFunctions;

			private BuilderFunctionRegistry() {
				super();
				this.mappingFunctions = new HashMap<>();
			}

			private BuilderFunctionRegistry(Map<Class<?>, BiFunction<String, ?, DataSource>> mappingFunctions) {
				super();
				this.mappingFunctions = Jdk8Utils.copyOfMap(mappingFunctions); // Under Java 11, this should be Map.copyOf(mappingFunctions);
				
			}

			public <T> void put(Class<? extends T> type, BiFunction<String, ? extends T, DataSource> mapper) {
				if (type == null)
					throw new NullPointerException("Type is null");
				mappingFunctions.put(type, mapper);
			}

			public <T> BiFunction<String, ? super T, DataSource> get(Class<? super T> type) {
				@SuppressWarnings("unchecked")
				BiFunction<String, ? super T, DataSource> function = (BiFunction<String, ? super T, DataSource>)mappingFunctions.get(type);
				return function != null ? function : null;
			}
			
			public static BuilderFunctionRegistry create() {
				return new BuilderFunctionRegistry();
			}
			
			public static BuilderFunctionRegistry from(Map<Class<?>, BiFunction<String, ?, DataSource>> mappingFunctions) {
				return new BuilderFunctionRegistry(mappingFunctions);
			}
		}	
	}
	
	/**
	 * Assists in pulling apart a DataSourceList into its component pieces.
	 *
	 */
	public static class Deconstructor {

		private final DataSourceList dsList;
		
		private Deconstructor(DataSourceList dsList) {
			this.dsList = dsList;
		}

		public static Deconstructor from(DataSourceList dsList) {
			return new Deconstructor(dsList);
		}

		/**
		 * Passthrough to DataSourceList.getDataSourceByName(String name)
		 * 
		 * @param name
		 * @return
		 */
		public final Optional<DataSource> getDataSourceByName(String name) {
			return dsList.getDataSourceByName(name);
		}

		/**
		 * Passthrough to DataSourceList.getDataSource(Predicate&lt;DataSource&gt; predicate)
		 * 
		 * @param predicate
		 * @return
		 */
		public final Optional<DataSource> getDataSource(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate);
		}

		/**
		 * Passthrough to DataSourceList.getDataSourcesByName(String name)
		 * 
		 * @param name
		 * @return
		 */
		public final List<DataSource> getDataSourcesByName(String name) {
			return dsList.getDataSourcesByName(name);
		}

		/**
		 * Passthrough to DataSourceList.getDataSources(Predicate&lt;DataSource&gt; predicate)
		 * 
		 * @param predicate
		 * @return
		 */
		public final List<DataSource> getDataSources(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate);
		}

		/**
		 * Convert contents of a DataSource to a String.  Assumes that the source input stream is UTF-8.
		 * 
		 * @param ds
		 * @return contents of the DataSource as a String object.
		 */
		public static final String dsToString(DataSource ds) {
			return dsToString(ds, StandardCharsets.UTF_8);	// Assume UTF-8
		}
		
		/**
		 * Convert contents of a DataSource to a String.  Assumes that the source input stream is in the character
		 * set provided.
		 * 
		 * @param ds
		 * @param cs
		 * @return
		 */
		public static final String dsToString(DataSource ds, Charset cs) {
			if (ds instanceof StringDataSource && cs == StringDataSource.ENCODING) {
				// Shortcut if this is already a StringDataSource
				return ((StringDataSource) ds).contents();
			}
			try (InputStream inputStream = ds.inputStream()) {
				return new String(Jdk8Utils.readAllBytes(inputStream), cs);
			} catch (IOException e) {
				throw new IllegalStateException("Error while converting DataSource to String", e);
			}
		}
		
		public final Optional<String> getStringByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToString);
		}

		public final Optional<String> getString(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToString);
		}

		public final List<String> getStringsByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToString)
					.collect(Collectors.toList());
		}

		public final List<String> getStrings(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToString)
					.collect(Collectors.toList());
		}

		public static final byte[] dsToByteArray(DataSource ds) {
			if (ds instanceof ByteArrayDataSource) {
				// Shortcut if this is already a ByteArrayDataSource
				return ((ByteArrayDataSource) ds).getContents();
			}
			try (InputStream inputStream = ds.inputStream()) {
				return Jdk8Utils.readAllBytes(inputStream);
			} catch (IOException e) {
				throw new IllegalStateException("Error while converting DataSource to ByteArray", e);
			}
			
		}
		
		public final Optional<byte[]> getByteArrayByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToByteArray);
		}

		public final Optional<byte[]> getByteArray(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToByteArray);
		}

		public final List<byte[]> getByteArraysByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToByteArray)
					.collect(Collectors.toList());
		}

		public final List<byte[]> getByteArrays(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToByteArray)
					.collect(Collectors.toList());
		}

		public static final Content dsToContent (DataSource ds) {
			return new Content(dsToByteArray(ds), ds.contentType());
		}
		
		public final Optional<Content> getContentByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToContent);
		}

		public final Optional<Content> getContent(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToContent);
		}

		public final List<Content> getContentsByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToContent)
					.collect(Collectors.toList());
		}

		public final List<Content> getContents(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToContent)
					.collect(Collectors.toList());
		}

		public static final FileContent dsToFileContent (DataSource ds) {
			return new FileContent(dsToByteArray(ds), ds.contentType(), ds.filename().orElseThrow(()->new UnsupportedOperationException("DataSource does not have filename, use Content object instead.")));
		}
		
		public final Optional<FileContent> getFileContentByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToFileContent);
		}

		public final Optional<FileContent> getFileContent(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToFileContent);
		}

		public final List<FileContent> getFileContentsByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToFileContent)
					.collect(Collectors.toList());
		}

		public final List<FileContent> getFileContents(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToFileContent)
					.collect(Collectors.toList());
		}

		public static final Boolean dsToBoolean(DataSource ds) {
			return Boolean.valueOf(dsToString(ds));
		}

		public final Optional<Boolean> getBooleanByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToBoolean);
		}

		public final Optional<Boolean> getBoolean(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToBoolean);
		}

		public final List<Boolean> getBooleansByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToBoolean)
					.collect(Collectors.toList());
		}

		public final List<Boolean> getBooleans(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToBoolean)
					.collect(Collectors.toList());
		}

		public static final Double dsToDouble(DataSource ds) {
			return Double.valueOf(dsToString(ds));
		}

		public final Optional<Double> getDoubleByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToDouble);
		}

		public final Optional<Double> getDouble(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToDouble);
		}

		public final List<Double> getDoublesByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToDouble)
					.collect(Collectors.toList());
		}

		public final List<Double> getDoubles(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToDouble)
					.collect(Collectors.toList());
		}

		public static final Float dsToFloat(DataSource ds) {
			return Float.valueOf(dsToString(ds));
		}

		public final Optional<Float> getFloatByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToFloat);
		}

		public final Optional<Float> getFloat(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToFloat);
		}

		public final List<Float> getFloatsByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToFloat)
					.collect(Collectors.toList());
		}

		public final List<Float> getFloats(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToFloat)
					.collect(Collectors.toList());
		}

		public static final Integer dsToInteger(DataSource ds) {
			return Integer.valueOf(dsToString(ds));
		}

		public final Optional<Integer> getIntegerByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToInteger);
		}

		public final Optional<Integer> getInteger(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToInteger);
		}

		public final List<Integer> getIntegersByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToInteger)
					.collect(Collectors.toList());
		}

		public final List<Integer> getIntegers(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToInteger)
					.collect(Collectors.toList());
		}

		public static final Long dsToLong(DataSource ds) {
			return Long.valueOf(dsToString(ds));
		}

		public final Optional<Long> getLongByName(String name) {
			return dsList.getDataSourceByName(name).map(Deconstructor::dsToLong);
		}

		public final Optional<Long> getLong(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(Deconstructor::dsToLong);
		}

		public final List<Long> getLongsByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToLong)
					.collect(Collectors.toList());
		}

		public final List<Long> getLongs(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToLong)
					.collect(Collectors.toList());
		}

		public static final Optional<DataSourceList> dsToDataSourceList(DataSource ds) {
			if (!XmlDataSourceListDecoder.DSL_MIME_TYPE.equals(ds.contentType())) {
				throw new IllegalArgumentException("Cannot convert DataSource with contentType '" + ds.contentType().asString() + "' to a DataSourceList.");
			}
			try(XmlDataSourceListDecoder decoder = XmlDataSourceListDecoder.wrap(ds.inputStream())) {
				return decoder.decode();
			} catch (XMLStreamException | IOException | FactoryConfigurationError e) {
				String msg = e.getMessage();
				throw new IllegalStateException("Error while decoding DataSourceList (" + (msg == null ? "null" : msg) + ").", e);
			}
		}

		public final Optional<DataSourceList> getDataSourceListByName(String name) {
			return dsList.getDataSourceByName(name).flatMap(Deconstructor::dsToDataSourceList);
		}

		public final Optional<DataSourceList> getDataSourceList(Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).flatMap(Deconstructor::dsToDataSourceList);
		}

		public final List<DataSourceList> getDataSourceListsByName(String name) {
			return dsList.getDataSourcesByName(name).stream()
					.map(Deconstructor::dsToDataSourceList)
					.flatMap(Jdk8Utils::optionalStream)
					.collect(Collectors.toList());
		}

		public final List<DataSourceList> getDataSourceLists(Predicate<DataSource> predicate) {
			return dsList.getDataSources(predicate).stream()
					.map(Deconstructor::dsToDataSourceList)
					.flatMap(Jdk8Utils::optionalStream)
					.collect(Collectors.toList());
		}
		
		// Default registry is a immutable registry containing all the standard deconstructor functions.
		private D11rFunctionRegistry defaultRegistry = D11rFunctionRegistry.from(StandardMappers.MAPPER_LIST.stream().collect(Collectors.toMap(Mapper::target, Mapper::from)));
		// Normal registry contains any deconstructor functions registered by the user.
		private D11rFunctionRegistry registry = D11rFunctionRegistry.create();
		
		private final <T> Function<DataSource, ? extends T> registryLookup(Class<? extends T> dest) {
			Function<DataSource, ? extends T> function = registry.get(dest);
			if (function != null) {
				return function;
			} else {
				Function<DataSource, ? extends T> defaultFunction = defaultRegistry.get(dest);
				if (defaultFunction != null) {
					return defaultFunction;
				} else {
					throw new UnsupportedOperationException("No mapping function found for class '" + dest.getName() + "'" );
				}
			}
		}
		
		public final <T> Deconstructor register(Class<? extends T> type, Function<DataSource, T> mapper) {
			registry.put(type, mapper);
			return this;
		}
		public final <T> Optional<T> getObject(Class<? extends T> dest, Predicate<DataSource> predicate) {
			return dsList.getDataSource(predicate).map(registryLookup(dest));
		}
		public final <T> Optional<T> getObjectByName(Class<? extends T> dest, String name) {
			return dsList.getDataSourceByName(name).map(registryLookup(dest));
		}
		public final <T> List<T> getObjects(Class<? extends T> dest, Predicate<DataSource> predicate) {
			Function<DataSource, ? extends T> mappingFn = registryLookup(dest);
			return dsList.getDataSources(predicate).stream()
					.map((Function<DataSource, ? extends T>)mappingFn)
					.collect(Collectors.toList());
		}
		public final <T> List<T> getObjectsByName(Class<? extends T> dest, String name) {
			Function<DataSource, ? extends T> mappingFn = registryLookup(dest);
			return dsList.getDataSourcesByName(name).stream()
					.map((Function<DataSource, ? extends T>)mappingFn)
					.collect(Collectors.toList());
		}
		
		private static class D11rFunctionRegistry {
			private Map<Class<?>, Function<DataSource, ?>> mappingFunctions;

			private D11rFunctionRegistry() {
				super();
				this.mappingFunctions = new HashMap<>();
			}

			private D11rFunctionRegistry(Map<Class<?>, Function<DataSource, ?>> mappingFunctions) {
				super();
				this.mappingFunctions = Jdk8Utils.copyOfMap(mappingFunctions); // Under Java 11, this should be Map.copyOf(mappingFunctions);
				
			}

			public <T> void put(Class<? extends T> type, Function<DataSource, T> mapper) {
				if (type == null)
					throw new NullPointerException("Type is null");
				mappingFunctions.put(type, mapper);
			}

			public <T> Function<DataSource, T> get(Class<T> type) {
				Function<DataSource, ?> function = mappingFunctions.get(type);
				return function != null ? ds->type.cast(function.apply(ds)) : null;
			}
			
			public static D11rFunctionRegistry create() {
				return new D11rFunctionRegistry();
			}
			
			public static D11rFunctionRegistry from(Map<Class<?>, Function<DataSource, ?>> mappingFunctions) {
				return new D11rFunctionRegistry(mappingFunctions);
			}
		}
	}
	
	/**
	 * Generic content, stored in memory with a mimetype
	 *
	 */
	public static class Content {
		protected final byte[] bytes;
		protected final MimeType mimeType;
		
		private Content(byte[] bytes, MimeType mimeType) {
			super();
			this.bytes = bytes;
			this.mimeType = mimeType;
		}

		public byte[] bytes() {
			return bytes;
		}

		public MimeType mimeType() {
			return mimeType;
		}

		public static Content from(byte[] ba, MimeType mimeType) {
			return new Content(ba, mimeType);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(bytes);
			result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Content other = (Content) obj;
			if (!Arrays.equals(bytes, other.bytes))
				return false;
			if (mimeType == null) {
				if (other.mimeType != null)
					return false;
			} else if (!mimeType.equals(other.mimeType))
				return false;
			return true;
		}
		
	}
	
	/**
	 * The bytes from a file (retains the filename)
	 *
	 */
	public static class FileContent extends Content {
		private final Path path;

		private FileContent(byte[] bytes, MimeType mimeType, Path path) {
			super(bytes, mimeType);
			this.path = path;
		}
		
		public Path path() {
			return path;
		}

		public static FileContent from(byte[] ba, MimeType mimeType, Path path) {
			return new FileContent(Objects.requireNonNull(ba, "content cannot be null"), Objects.requireNonNull(mimeType, "MimeType cannot be null"), Objects.requireNonNull(path, "Path cannot be null"));
		}
		public static FileContent from(byte[] ba, Path path, MimeTypeFileTypeMap map) {
			return from(ba, determineMimeType(path, map), path);
		}
		public static FileContent from(byte[] ba, Path path) {
			return from(ba, path, UnmodifiableFileExtensionsMap.DEFAULT_MAP);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileContent other = (FileContent) obj;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}
	}
	
	private static MimeType determineMimeType(Path filePath, MimeTypeFileTypeMap map) {
		return map.mimeType(filePath).orElse(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE);
	}	
	
	public static interface Mapper<T> {
		public BiFunction<String, T, DataSource> to();
		public Function<DataSource, T> from();
		public Class<T> target();
	}

	public static class StandardMappers {
		public static Mapper<String> STRING = new Mapper<String>() {
			
			@Override
			public BiFunction<String, String, DataSource> to() { return Builder::<String>objToStringDS; }
			
			@Override
			public Function<DataSource, String> from() { return Deconstructor::dsToString; }
			
			@Override
			public Class<String> target() { return String.class; }
		};

		public static Mapper<String> createStringMapper(Charset cs) {
			return new Mapper<String>() {

				@Override
				public BiFunction<String, String, DataSource> to() { return Builder::<String>objToStringDS; }

				@Override
				public Function<DataSource, String> from() { return ds->Deconstructor.dsToString(ds, cs); }

				@Override
				public Class<String> target() { return String.class; }
			};
		}
		
		public static Mapper<byte[]> BYTEARRAY = new Mapper<byte[]>() {

			@Override
			public BiFunction<String, byte[], DataSource> to() { return (name, ba)->new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null.")); }

			@Override
			public Function<DataSource, byte[]> from() { return Deconstructor::dsToByteArray; }

			@Override
			public Class<byte[]> target() { return byte[].class; }
		};
		
		public static Mapper<byte[]> createByteArrayMapper(MimeType mimeType) {
			return new Mapper<byte[]>() {

				@Override
				public BiFunction<String, byte[], DataSource> to() { return (name, ba)->new ByteArrayDataSource(ba, Objects.requireNonNull(name, "Name cannot be null."), mimeType); }

				@Override
				public Function<DataSource, byte[]> from() { return Deconstructor::dsToByteArray; }

				@Override
				public Class<byte[]> target() { return byte[].class; }
			};
		}
		
		public static Mapper<Content> CONTENT = new Mapper<Content>() {

			@Override
			public BiFunction<String, Content, DataSource> to() { return (name, c)->new ByteArrayDataSource(c.bytes, Objects.requireNonNull(name, "Name cannot be null."), c.mimeType); }

			@Override
			public Function<DataSource, Content> from() { return Deconstructor::dsToContent; }

			@Override
			public Class<Content> target() { return Content.class; }
		};
		
		public static Mapper<FileContent> FILE_CONTENT = new Mapper<FileContent>() {

			@Override
			public BiFunction<String, FileContent, DataSource> to() { return (name, fc)->new ByteArrayDataSource(fc.bytes, Objects.requireNonNull(name, "Name cannot be null."), fc.mimeType); }

			@Override
			public Function<DataSource, FileContent> from() { return Deconstructor::dsToFileContent; }

			@Override
			public Class<FileContent> target() { return FileContent.class; }
		};
		
		public static Mapper<Path> PATH = new Mapper<Path>() {

			@Override
			public BiFunction<String, Path, DataSource> to() { return (name, p)->new FileDataSource(p, Objects.requireNonNull(name, "Name cannot be null.")); }

			@Override
			public Function<DataSource, Path> from() { return ds->unsupportedOperation(Path.class, "Cannot convert DataSource directly to %s - use String instead and then convert String to Path."); }

			@Override
			public Class<Path> target() { return Path.class; }
		};
		
		public static Mapper<Boolean> BOOLEAN = new Mapper<Boolean>() {

			@Override
			public BiFunction<String, Boolean, DataSource> to() { return Builder::<Boolean>objToStringDS; }

			@Override
			public Function<DataSource, Boolean> from() { return Deconstructor::dsToBoolean; }

			@Override
			public Class<Boolean> target() { return Boolean.class; }
		};
		
		public static Mapper<Double> DOUBLE = new Mapper<Double>() {

			@Override
			public BiFunction<String, Double, DataSource> to() { return Builder::<Double>objToStringDS; }

			@Override
			public Function<DataSource, Double> from() { return Deconstructor::dsToDouble; }

			@Override
			public Class<Double> target() { return Double.class; }
		};
		
		public static Mapper<Float> FLOAT = new Mapper<Float>() {

			@Override
			public BiFunction<String, Float, DataSource> to()  { return Builder::<Float>objToStringDS; }

			@Override
			public Function<DataSource, Float> from() { return Deconstructor::dsToFloat; }

			@Override
			public Class<Float> target() { return Float.class; }
		};
		
		public static Mapper<Integer> INTEGER = new Mapper<Integer>() {

			@Override
			public BiFunction<String, Integer, DataSource> to() { return Builder::<Integer>objToStringDS; }

			@Override
			public Function<DataSource, Integer> from() { return Deconstructor::dsToInteger; }

			@Override
			public Class<Integer> target() { return Integer.class; }
		};
		
		public static Mapper<Long> LONG = new Mapper<Long>() {

			@Override
			public BiFunction<String, Long, DataSource> to() { return Builder::<Long>objToStringDS; }

			@Override
			public Function<DataSource, Long> from() { return Deconstructor::dsToLong; }

			@Override
			public Class<Long> target() { return Long.class; }
		};
		
		public static Mapper<DataSource> DATASOURCE = new Mapper<DataSource>() {

			@Override
			public BiFunction<String, DataSource, DataSource> to() { return (s,ds)->ds; }	// Ignore the name that's passed in because the DS already has one.

			@Override
			public Function<DataSource, DataSource> from() { return Function.identity(); }

			@Override
			public Class<DataSource> target() { return DataSource.class; }
		};
		
		public static Mapper<DataSourceList> DATASOURCELIST = new Mapper<DataSourceList>() {

			@Override
			public BiFunction<String, DataSourceList, DataSource> to() { return (name, dsl)->new ByteArrayDataSource(Builder.dataSourceListToByteArray(dsl), Objects.requireNonNull(name, "Name cannot be null."), XmlDataSourceListEncoder.DSL_MIME_TYPE); }

			@Override
			public Function<DataSource, DataSourceList> from() { return ds->Deconstructor.dsToDataSourceList(ds).orElseThrow(()->new IllegalStateException("Unable to decode DataSource '" + ds.name() + "' into DataSourceList.")); }

			@Override
			public Class<DataSourceList> target() { return DataSourceList.class; }
		};
		
		public static List<Mapper<?>> MAPPER_LIST = Jdk8Utils.listOf(STRING, BYTEARRAY, CONTENT, FILE_CONTENT, PATH, BOOLEAN, DOUBLE, FLOAT, INTEGER, LONG, DATASOURCE, DATASOURCELIST);
		
		private static <T> T unsupportedOperation(Class<T> clazz, String msg) {
			throw new UnsupportedOperationException(String.format(msg, clazz.getName()));
		}
		
		private StandardMappers() { // prevent instantation
			super();
		}
	}
}
