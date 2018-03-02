package nl.jchmb.fxutils.binding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * The ListBinder class allows one to bind a list of one type to a list of another type.
 * This is especially useful when you want to bind a list of items of some arbitrary type
 * to a list of Node instances.
 * 
 * The ListBinder can also keep track of changes in the items themselves. This will require
 * one to register property getters (functions that map an item to the desired property).
 * 
 * @param <T> source type
 * @param <U> target type
 */
public class ListBinder<T, U> {
	private ListBinding<U> binding;
	private final ListProperty<T> list;
	private final ListProperty<U> mirrorList;
	private final List<Function<T, Property<?>>> propertyGetters;
	private final Function<T, U> mapper;
	private final Predicate<T> filter;
	private final Comparator<T> comparator;
	private final List<Property<?>> extraProperties;
	private final Set<T> knownItems;
	private final ListChangeListener<T> listener =
		(Change<? extends T> c) -> registerItems();
	
	/**
	 * ListBinder constructor.
	 * @param list
	 * @param mapper
	 * @param filter
	 * @param comparator
	 */
	public ListBinder(
			final ListProperty<T> list,
			final Function<T, U> mapper,
			final Predicate<T> filter,
			final Comparator<T> comparator
	) {
		this.list = list;
		this.mirrorList = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.list.addListener(listener);
		this.filter = filter;
		this.mapper = mapper;
		this.comparator = comparator;
		this.propertyGetters = new ArrayList<>();
		this.extraProperties = new ArrayList<>();
		this.knownItems = new HashSet<>();
	}
	
	/**
	 * Call this to perform the actual binding, assuming that extra properties and 
	 * property getters have been registered to the ListBinder.
	 */
	public final void bind() {
		this.binding = new ListBinding<>() {
			{
				super.bind(list);
				extraProperties.forEach(super::bind);
			}
			
			@Override
			protected ObservableList<U> computeValue() {
				return FXCollections.observableArrayList(
					list.stream()
						.filter(filter)
						.sorted(comparator)
						.map(mapper)
						.collect(Collectors.toList())
				);
			}
				
		};
		this.mirrorList.bind(binding);
	}
	
	public final void unbind() {
		this.mirrorList.unbind();
		this.binding = null;
	}
	
	/**
	 * Add an extra property that trigger a change in the target list when changed.
	 * @param property
	 */
	public final ListBinder<T, U> addExtraProperty(final Property<?> property) {
		extraProperties.add(property);
		return this;
	}
	
	/**
	 * Add a property getter.
	 * @param propertyGetter
	 * @return
	 */
	public final ListBinder<T, U> addPropertyGetter(final Function<T, Property<?>> propertyGetter) {
		propertyGetters.add(propertyGetter);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public final ListBinder<T, U> addPropertyGetters(
			final Function<T, Property<?>>... propertyGetters
	) {
		for (final Function<T, Property<?>> propertyGetter : propertyGetters) {
			addPropertyGetter(propertyGetter);
		}
		return this;
	}
	
	/**
	 * Register the items.
	 */
	private final void registerItems() {
		for (final T item : list) {
			if (knownItems.contains(item)) {
				continue;
			}
			registerItem(item);
			knownItems.add(item);
		}
	}
	
	/**
	 * Update the list by unbinding-binding.
	 */
	private final void update() {
		this.unbind();
		this.bind();
	}
	
	/**
	 * Register a new item to watch.
	 * @param item
	 */
	private final void registerItem(final T item) {
		for (final Function<T, Property<?>> propertyGetter : propertyGetters) {
			propertyGetter.apply(item)
				.addListener(
					(prop, ov, nv) -> {
						update();
					}
				);
		}
	}
	
	/**
	 * Get the mirror (target) list property.
	 * @return
	 */
	public final ListProperty<U> mirrorProperty() {
		return mirrorList;
	}
	
	/**
	 * Convenience function to get a new ListBinder for Node lists. This can be called
	 * in conjunction with Parent objects that expose the Parent#getChildren method as public.
	 * This method allows sorting and filtering in the target list independent of the source list.
	 * @param list
	 * @param nodeList
	 * @param mapper
	 * @param filter
	 * @param comparator
	 * @param extraProperties
	 * @return
	 */
	public static final <T> ListBinder<T, Node> nodeListBinder(
			final ListProperty<T> list,
			final ObservableList<Node> nodeList,
			final Function<T, Node> mapper,
			final Predicate<T> filter,
			final Comparator<T> comparator,
			final Property<?>... extraProperties
	) {
		final ListBinder<T, Node> binder = new ListBinder<>(
			list,
			mapper,
			filter,
			comparator
		);
		for (final Property<?> extraProperty : extraProperties) {
			binder.addExtraProperty(extraProperty);
		}
		Bindings.bindContent(
			nodeList,
			binder.mirrorProperty()
		);
		return binder;
	}
	
	/**
	 * Convenience function to get a new ListBinder for Node lists. This can be called
	 * in conjunction with Parent objects that expose the Parent#getChildren method as public.
	 * @param list
	 * @param nodeList
	 * @param mapper
	 * @param extraProperties
	 * @return
	 */
	public static final <T> ListBinder<T, Node> nodeListBinder(
			final ListProperty<T> list,
			final ObservableList<Node> nodeList,
			final Function<T, Node> mapper,
			final Property<?>... extraProperties
	) {
		return nodeListBinder(
			list,
			nodeList,
			mapper,
			x -> true,
			(x, y) -> 0,
			extraProperties
		);
	}
}
