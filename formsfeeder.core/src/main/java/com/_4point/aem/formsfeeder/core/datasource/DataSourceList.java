package com._4point.aem.formsfeeder.core.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

/**
 * Wraps a list of DataSource objects and provides common functions for operating on that list.
 *
 */
public class DataSourceList {
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
		return list;
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
	 * @return true if list is empty otherwise false
	 */
	public final boolean isEmpty() {
		return this.list().isEmpty();
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

		public DataSourceList build() {
			return DataSourceList.from(underConstruction);
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
			// TODO:  Add DataSourceList to the List
//			underConstruction.add(new StringDataSource(Long.toString(l), Objects.requireNonNull(name, "Name cannot be null.")));
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
			// TODO: Add the DataSourceList to the current list.
//			underConstruction.add(new StringDataSource(Long.toString(l), Objects.requireNonNull(name, "Name cannot be null."), attributes));
			return this;
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
			// TODO: Add the DataSourceLists
//			lList.forEach(l->underConstruction.add(new StringDataSource(l.toString(), Objects.requireNonNull(name, "Name cannot be null."))));
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
			// TODO:  Add DataSourceLists to the list.
//			lList.forEach(l->underConstruction.add(new StringDataSource(l.toString(), Objects.requireNonNull(name, "Name cannot be null."), attributes)));
			return this;
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
			if (ds instanceof StringDataSource) {
				// Shortcut if this is already a StringDataSource
				return ((StringDataSource) ds).contents();
			}
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

		public static final DataSourceList dsToDataSourceList(DataSource ds) {
//			return Long.valueOf(dsToString(ds));
			// TODO:  Implement this.
			return null;
		}

		public final Optional<DataSourceList> getDataSourceListByName(String name) {
//			return dsList.getDataSourceByName(name).map(Deconstructor::dsToLong);
			// TODO:  Implement this.
			return null;
		}

		public final Optional<DataSourceList> getDataSourceList(Predicate<DataSource> predicate) {
//			return dsList.getDataSource(predicate).map(Deconstructor::dsToLong);
			// TODO:  Implement this.
			return null;
		}

		public final List<DataSourceList> getDataSourceListsByName(String name) {
//			return dsList.getDataSourcesByName(name).stream()
//					.map(Deconstructor::dsToLong)
//					.collect(Collectors.toList());
			// TODO:  Implement this.
			return null;
		}

		public final List<DataSourceList> getDataSourceLists(Predicate<DataSource> predicate) {
//			return dsList.getDataSources(predicate).stream()
//					.map(Deconstructor::dsToLong)
//					.collect(Collectors.toList());
			// TODO:  Implement this.
			return null;
		}


	}
}
