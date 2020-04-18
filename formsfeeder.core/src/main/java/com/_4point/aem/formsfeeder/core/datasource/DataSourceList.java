package com._4point.aem.formsfeeder.core.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Wraps a list of DataSource objects and provides common functions for operating on that list.
 *
 */
public class DataSourceList {
	private final List<DataSource> list;

	private DataSourceList(List<DataSource> list) {
		this.list = List.copyOf(list);
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
	 * Gets the first DataSource with the specified name.
	 * 
	 * Will generate a NullPointerException if the name parameter is null.
	 * 
	 * Shortcut function for <code>getDataSource((ds)->ds.name().equals(name))</code>
	 * 
	 * @param name
	 * @return 
	 */
	public final Optional<DataSource> getDataSourceByName(final String name) {
		return getDataSource((ds)->ds.name().equals(Objects.requireNonNull(name, "Target DataSource name cannot be null.")));
	}

	/**
	 * Gets the first DataSource which where the predicate provided evaluates to true.
	 * 
	 * Will generate a NullPointerException if the predicate parameter is null.
	 * 
	 * @param predicate
	 * @return
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
	 * Shortcut function for <code>getDataSources((ds)->ds.name().equals(name))</code>
	 * 
	 * @param name
	 * @return
	 */
	public final List<DataSource> getDataSourcesByName(String name) {
		return getDataSources((ds)->ds.name().equals(Objects.requireNonNull(name, "Target DataSource name cannot be null.")));
	}

	/**
	 * Gets a list of DataSource objects where the predicate provided evaluates to true.
	 * 
	 * Will generate a NullPointerException if the predicate parameter is null.
	 * 
	 * @param predicate
	 * @return
	 */
	public final List<DataSource> getDataSources(final Predicate<DataSource> predicate) {
		Objects.requireNonNull(predicate, "Predicate provided cannot be null.");
		ArrayList<DataSource> found = new ArrayList<>();
		for (DataSource ds : list) {
			if (predicate.test(ds)) {
				found.add(ds);
			}
		}
		return List.copyOf(found);
	}

	/**
	 * Static constructor for DataSourceList.  A defensive copy is made of the list, so that subsequent changes
	 * do not affect the list.
	 * 
	 * @param list
	 * @return 
	 */
	public static DataSourceList from(List<DataSource> list) {
		return new DataSourceList(list);
	}
}
