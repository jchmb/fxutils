JavaFX Utils Library
====================

Code Sample
-----------

```java
final Group group = new Group();
final ListProperty<Person> listProperty = ...; // Some list property with Person objects
final ListBinder<Person, Node> binder = ListBinder.nodeListBinder(
	listProperty,
	group.getChildren(),
	person -> new Label(person.getName())
);

// Changing the Person's name property now triggers a change in the list.
binder.addPropertyGetter(Person::nameProperty);

// Do the actual binding to finish things up.
binder.bind();
```