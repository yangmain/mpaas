/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghost.framework.data.commons.converter;

import ghost.framework.beans.annotation.constraints.Nullable;
import ghost.framework.context.converter.TypeConverter;
import ghost.framework.context.converter.ConverterFactory;
import ghost.framework.context.converter.GenericTypeDescriptorConverter;
import ghost.framework.context.converter.annotation.ReadingConverter;
import ghost.framework.context.converter.annotation.WritingConverter;
import ghost.framework.context.core.GenericTypeResolver;
import ghost.framework.core.converter.GenericConversionService;
import ghost.framework.data.commons.mapping.model.SimpleTypeHolder;
import ghost.framework.data.commons.util.Streamable;
import ghost.framework.util.Assert;
import ghost.framework.util.ObjectUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Value object to capture custom conversion. That is essentially a {@link List} of converters and some additional logic
 * around them. The converters build up two sets of types which store-specific basic types can be converted into and
 * from. These types will be considered simple ones (which means they neither need deeper inspection nor nested
 * conversion. Thus the {@link CustomConversions} also act as factory for {@link SimpleTypeHolder} .
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.0
 */
public class CustomConversions {

	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CustomConversions.class);
	private static final String READ_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as reading converter although it doesn't convert from a store-supported type! You might want to check your annotation setup at the converter implementation.";
	private static final String WRITE_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as writing converter although it doesn't convert to a store-supported type! You might want to check your annotation setup at the converter implementation.";
	private static final String NOT_A_CONVERTER = "TypeConverter %s is neither a Spring TypeConverter, GenericTypeDescriptorConverter or ConverterFactory!";
	private static final String CONVERTER_FILTER = "converter from %s to %s as %s converter.";
	private static final String ADD_CONVERTER = "Adding %s" + CONVERTER_FILTER;
	private static final String SKIP_CONVERTER = "Skipping " + CONVERTER_FILTER
			+ " %s is not a store supported simple type!";
	private static final List<Object> DEFAULT_CONVERTERS;

	static {

		List<Object> defaults = new ArrayList<>();

//		defaults.addAll(JodaTimeConverters.getConvertersToRegister());
//		defaults.addAll(Jsr310Converters.getConvertersToRegister());
//		defaults.addAll(ThreeTenBackPortConverters.getConvertersToRegister());

		DEFAULT_CONVERTERS = Collections.unmodifiableList(defaults);
	}

	private final SimpleTypeHolder simpleTypeHolder;
	private final List<Object> converters;

	private final Set<GenericTypeDescriptorConverter.ConvertiblePair> readingPairs = new LinkedHashSet<>();
	private final Set<GenericTypeDescriptorConverter.ConvertiblePair> writingPairs = new LinkedHashSet<>();
	private final Set<Class<?>> customSimpleTypes = new HashSet<>();
	private final ConversionTargetsCache customReadTargetTypes = new ConversionTargetsCache();
	private final ConversionTargetsCache customWriteTargetTypes = new ConversionTargetsCache();

	private final ConverterConfiguration converterConfiguration;

	private final Function<GenericTypeDescriptorConverter.ConvertiblePair, Class<?>> getReadTarget = convertiblePair -> getCustomTarget(
			convertiblePair.getSourceType(), convertiblePair.getTargetType(), readingPairs);

	private final Function<GenericTypeDescriptorConverter.ConvertiblePair, Class<?>> getWriteTarget = convertiblePair -> getCustomTarget(
			convertiblePair.getSourceType(), convertiblePair.getTargetType(), writingPairs);

	private final Function<GenericTypeDescriptorConverter.ConvertiblePair, Class<?>> getRawWriteTarget = convertiblePair -> getCustomTarget(
			convertiblePair.getSourceType(), null, writingPairs);

	/**
	 * @param converterConfiguration the {@link ConverterConfiguration} to apply.
	 * @since 2.3
	 */
	public CustomConversions(ConverterConfiguration converterConfiguration) {

		this.converterConfiguration = converterConfiguration;

		List<Object> registeredConverters = collectPotentialConverterRegistrations(
				converterConfiguration.getStoreConversions(), converterConfiguration.getUserConverters()).stream() //
						.filter(this::isSupportedConverter) //
						.filter(this::shouldRegister) //
						.map(ConverterRegistrationIntent::getConverterRegistration) //
						.map(this::register) //
						.distinct() //
						.collect(Collectors.toList());

		Collections.reverse(registeredConverters);

		this.converters = Collections.unmodifiableList(registeredConverters);
		this.simpleTypeHolder = new SimpleTypeHolder(customSimpleTypes,
				converterConfiguration.getStoreConversions().getStoreTypeHolder());
	}

	/**
	 * Creates a new {@link CustomConversions} instance registering all given user defined converters and selecting
	 * {@link TypeConverter converters} from {@link StoreConversions} depending on
	 * {@link StoreConversions#getSimpleTypeHolder() store simple types} only considering those that either convert
	 * to/from a store supported type.
	 *
	 * @param storeConversions must not be {@literal null}.
	 * @param converters must not be {@literal null}.
	 */
	public CustomConversions(StoreConversions storeConversions, Collection<?> converters) {
		this(new ConverterConfiguration(storeConversions, new ArrayList<>(converters)));
	}

	/**
	 * Returns the underlying {@link SimpleTypeHolder}.
	 *
	 * @return
	 */
	public SimpleTypeHolder getSimpleTypeHolder() {
		return simpleTypeHolder;
	}

	/**
	 * Returns whether the given type is considered to be simple. That means it's either a general simple type or we have
	 * a writing {@link TypeConverter} registered for a particular type.
	 *
	 * @see SimpleTypeHolder#isSimpleType(Class)
	 * @param type
	 * @return
	 */
	public boolean isSimpleType(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return simpleTypeHolder.isSimpleType(type);
	}

	/**
	 * Populates the given {@link GenericConversionService} with the converters registered.
	 *
	 * @param conversionService
	 */
	public void registerConvertersIn(Object conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null!");
		converters.forEach(it -> registerConverterIn(it, conversionService));
	}

	/**
	 * Get all converters and add origin information
	 *
	 * @param storeConversions
	 * @param converters
	 * @return
	 * @since 2.3
	 */
	private List<ConverterRegistrationIntent> collectPotentialConverterRegistrations(StoreConversions storeConversions,
			Collection<?> converters) {

		List<ConverterRegistrationIntent> converterRegistrations = new ArrayList<>();

		converters.stream() //
				.map(storeConversions::getRegistrationsFor) //
				.flatMap(Streamable::stream) //
				.map(ConverterRegistrationIntent::userConverters) //
				.forEach(converterRegistrations::add);

		storeConversions.getStoreConverters().stream() //
				.map(storeConversions::getRegistrationsFor) //
				.flatMap(Streamable::stream) //
				.map(ConverterRegistrationIntent::storeConverters) //
				.forEach(converterRegistrations::add);

		DEFAULT_CONVERTERS.stream() //
				.map(storeConversions::getRegistrationsFor) //
				.flatMap(Streamable::stream) //
				.map(ConverterRegistrationIntent::defaultConverters) //
				.forEach(converterRegistrations::add);

		return converterRegistrations;
	}
	/**
	 * Registers the given converter in the given {@link GenericConversionService}.
	 *
	 * @param candidate must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	private void registerConverterIn(Object candidate, Object conversionService) {
		if (candidate instanceof TypeConverter) {
//			conversionService.addConverter(TypeConverter.class.cast(candidate));
			return;
		}

		if (candidate instanceof ConverterFactory) {
//			conversionService.addConverterFactory(ConverterFactory.class.cast(candidate));
			return;
		}

		if (candidate instanceof GenericTypeDescriptorConverter) {
//			conversionService.addConverter(GenericTypeDescriptorConverter.class.cast(candidate));
			return;
		}

		if (candidate instanceof ConverterBuilder.ConverterAware) {
//			ConverterBuilder.ConverterAware.class.cast(candidate).getConverters().forEach(it -> registerConverterIn(it, conversionService));
			return;
		}

		throw new IllegalArgumentException(String.format(NOT_A_CONVERTER, candidate));
	}

	/**
	 * Registers the given {@link ConvertiblePair} as reading or writing pair depending on the type sides being basic
	 * types.
	 *
	 * @param converterRegistration
	 */
	private Object register(ConverterRegistration converterRegistration) {

		Assert.notNull(converterRegistration, "TypeConverter registration must not be null!");

		GenericTypeDescriptorConverter.ConvertiblePair pair = converterRegistration.getConvertiblePair();

		if (converterRegistration.isReading()) {

			readingPairs.add(pair);

			if (LOG.isWarnEnabled() && !converterRegistration.isSimpleSourceType()) {
				LOG.warn(String.format(READ_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
			}
		}

		if (converterRegistration.isWriting()) {

			writingPairs.add(pair);
			customSimpleTypes.add(pair.getSourceType());

			if (LOG.isWarnEnabled() && !converterRegistration.isSimpleTargetType()) {
				LOG.warn(String.format(WRITE_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
			}
		}

		return converterRegistration.getConverter();
	}

	/**
	 * Validate a given {@link ConverterRegistration} in a specific setup. <br />
	 * Non {@link ReadingConverter reading} and user defined {@link TypeConverter converters} are only considered supported if
	 * the {@link ConverterRegistrationIntent#isSimpleTargetType() target type} is considered to be a store simple type.
	 *
	 * @param registrationIntent
	 * @return {@literal true} if supported.
	 * @since 2.3
	 */
	private boolean isSupportedConverter(ConverterRegistrationIntent registrationIntent) {

		boolean register = registrationIntent.isUserConverter() || registrationIntent.isStoreConverter()
				|| (registrationIntent.isReading() && registrationIntent.isSimpleSourceType())
				|| (registrationIntent.isWriting() && registrationIntent.isSimpleTargetType());

		if (LOG.isDebugEnabled()) {

			if (register) {
				LOG.debug(String.format(ADD_CONVERTER, registrationIntent.isUserConverter() ? "user defined " : "",
						registrationIntent.getSourceType(), registrationIntent.getTargetType(),
						registrationIntent.isReading() ? "reading" : "writing"));
			} else {
				LOG.debug(String.format(SKIP_CONVERTER, registrationIntent.getSourceType(), registrationIntent.getTargetType(),
						registrationIntent.isReading() ? "reading" : "writing",
						registrationIntent.isReading() ? registrationIntent.getSourceType() : registrationIntent.getTargetType()));
			}
		}

		return register;
	}

	/**
	 * @param intent must not be {@literal null}.
	 * @return {@literal false} if the given {@link ConverterRegistration} shall be skipped.
	 * @since 2.3
	 */
	private boolean shouldRegister(ConverterRegistrationIntent intent) {
		return !intent.isDefaultConverter()
				|| converterConfiguration.shouldRegister(intent.getConverterRegistration().getConvertiblePair());
	}

	/**
	 * Returns the target type to convert to in case we have a custom conversion registered to convert the given source
	 * type into a store native one.
	 *
	 * @param sourceType must not be {@literal null}
	 * @return
	 */
	public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {

		Assert.notNull(sourceType, "Source type must not be null!");

		Class<?> target = customWriteTargetTypes.computeIfAbsent(sourceType, getRawWriteTarget);

		return Void.class.equals(target) || target == null ? Optional.empty() : Optional.of(target);
	}

	/**
	 * Returns the target type we can read an inject of the given source type to. The returned type might be a subclass of
	 * the given expected type though. If {@code requestedTargetType} is {@literal null} we will simply return the first
	 * target type matching or {@literal null} if no conversion can be found.
	 *
	 * @param sourceType must not be {@literal null}
	 * @param requestedTargetType must not be {@literal null}.
	 * @return
	 */
	public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType, Class<?> requestedTargetType) {

		Assert.notNull(sourceType, "Source type must not be null!");
		Assert.notNull(requestedTargetType, "Target type must not be null!");

		Class<?> target = customWriteTargetTypes.computeIfAbsent(sourceType, requestedTargetType, getWriteTarget);

		return Void.class.equals(target) || target == null ? Optional.empty() : Optional.of(target);
	}

	/**
	 * Returns whether we have a custom conversion registered to read {@code sourceType} into a native type. The returned
	 * type might be a subclass of the given expected type though.
	 *
	 * @param sourceType must not be {@literal null}
	 * @return
	 */
	public boolean hasCustomWriteTarget(Class<?> sourceType) {

		Assert.notNull(sourceType, "Source type must not be null!");

		return getCustomWriteTarget(sourceType).isPresent();
	}

	/**
	 * Returns whether we have a custom conversion registered to read an object of the given source type into an object of
	 * the given native target type.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return
	 */
	public boolean hasCustomWriteTarget(Class<?> sourceType, Class<?> targetType) {

		Assert.notNull(sourceType, "Source type must not be null!");
		Assert.notNull(targetType, "Target type must not be null!");

		return getCustomWriteTarget(sourceType, targetType).isPresent();
	}

	/**
	 * Returns whether we have a custom conversion registered to read the given source into the given target type.
	 *
	 * @param sourceType must not be {@literal null}
	 * @param targetType must not be {@literal null}
	 * @return
	 */
	public boolean hasCustomReadTarget(Class<?> sourceType, Class<?> targetType) {

		Assert.notNull(sourceType, "Source type must not be null!");
		Assert.notNull(targetType, "Target type must not be null!");

		return getCustomReadTarget(sourceType, targetType) != null;
	}

	/**
	 * Returns the actual target type for the given {@code sourceType} and {@code targetType}. Note that the returned
	 * {@link Class} could be an assignable type to the given {@code targetType}.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private Class<?> getCustomReadTarget(Class<?> sourceType, Class<?> targetType) {
		return customReadTargetTypes.computeIfAbsent(sourceType, targetType, getReadTarget);
	}

	/**
	 * Inspects the given {@link ConvertiblePair ConvertiblePairs} for ones that have a source compatible type as source.
	 * Additionally checks assignability of the target type if one is given.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @param targetType can be {@literal null}.
	 * @param pairs must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private Class<?> getCustomTarget(Class<?> sourceType, @Nullable Class<?> targetType,
			Collection<GenericTypeDescriptorConverter.ConvertiblePair> pairs) {

		if (targetType != null && pairs.contains(new GenericTypeDescriptorConverter.ConvertiblePair(sourceType, targetType))) {
			return targetType;
		}

		for (GenericTypeDescriptorConverter.ConvertiblePair pair : pairs) {

			if (!hasAssignableSourceType(pair, sourceType)) {
				continue;
			}

			Class<?> candidate = pair.getTargetType();

			if (!requestedTargetTypeIsAssignable(targetType, candidate)) {
				continue;
			}

			return candidate;
		}

		return null;
	}

	private static boolean hasAssignableSourceType(GenericTypeDescriptorConverter.ConvertiblePair pair, Class<?> sourceType) {
		return pair.getSourceType().isAssignableFrom(sourceType);
	}

	private static boolean requestedTargetTypeIsAssignable(@Nullable Class<?> requestedTargetType, Class<?> targetType) {
		return requestedTargetType == null || targetType.isAssignableFrom(requestedTargetType);
	}

	/**
	 * Value object to cache custom conversion targets.
	 *
	 * @author Mark Paluch
	 */
	static class ConversionTargetsCache {

		private final Map<Class<?>, TargetTypes> customReadTargetTypes = new ConcurrentHashMap<>();

		/**
		 * Get or compute a target type given its {@code sourceType}. Returns a cached {@link Optional} if the value
		 * (present/absent target) was computed once. Otherwise, uses a {@link Function mappingFunction} to determine a
		 * possibly existing target type.
		 *
		 * @param sourceType must not be {@literal null}.
		 * @param mappingFunction must not be {@literal null}.
		 * @return the optional target type.
		 */
		@Nullable
		public Class<?> computeIfAbsent(Class<?> sourceType, Function<GenericTypeDescriptorConverter.ConvertiblePair, Class<?>> mappingFunction) {
			return computeIfAbsent(sourceType, AbsentTargetTypeMarker.class, mappingFunction);
		}

		/**
		 * Get or compute a target type given its {@code sourceType} and {@code targetType}. Returns a cached
		 * {@link Optional} if the value (present/absent target) was computed once. Otherwise, uses a {@link Function
		 * mappingFunction} to determine a possibly existing target type.
		 *
		 * @param sourceType must not be {@literal null}.
		 * @param targetType must not be {@literal null}.
		 * @param mappingFunction must not be {@literal null}.
		 * @return the optional target type.
		 */
		@Nullable
		public Class<?> computeIfAbsent(Class<?> sourceType, Class<?> targetType,
				Function<GenericTypeDescriptorConverter.ConvertiblePair, Class<?>> mappingFunction) {

			TargetTypes targetTypes = customReadTargetTypes.get(sourceType);

			if (targetTypes == null) {
				targetTypes = customReadTargetTypes.computeIfAbsent(sourceType, TargetTypes::new);
			}

			return targetTypes.computeIfAbsent(targetType, mappingFunction);
		}

		/**
		 * Marker type for absent target type caching.
		 */
		interface AbsentTargetTypeMarker {}
	}

	/**
	 * Value object for a specific {@code Class source type} to determine possible target conversion types.
	 *
	 * @author Mark Paluch
	 */
	static class TargetTypes {

		private final Class<?> sourceType;
		private final Map<Class<?>, Class<?>> conversionTargets = new ConcurrentHashMap<>();

		TargetTypes(Class<?> sourceType) {
			this.sourceType = sourceType;
		}

		/**
		 * Get or compute a target type given its {@code targetType}. Returns a cached {@link Optional} if the value
		 * (present/absent target) was computed once. Otherwise, uses a {@link Function mappingFunction} to determine a
		 * possibly existing target type.
		 *
		 * @param targetType must not be {@literal null}.
		 * @param mappingFunction must not be {@literal null}.
		 * @return the optional target type.
		 */
		@Nullable
		public Class<?> computeIfAbsent(Class<?> targetType, Function<GenericTypeDescriptorConverter.ConvertiblePair, Class<?>> mappingFunction) {

			Class<?> optionalTarget = conversionTargets.get(targetType);

			if (optionalTarget == null) {
				optionalTarget = mappingFunction.apply(new GenericTypeDescriptorConverter.ConvertiblePair(sourceType, targetType));
				conversionTargets.put(targetType, optionalTarget == null ? Void.class : optionalTarget);
			}

			return Void.class.equals(optionalTarget) ? null : optionalTarget;
		}
	}

	/**
	 * Value class tying together a {@link ConverterRegistration} and its {@link ConverterOrigin origin} to allow fine
	 * grained registration based on store supported types.
	 *
	 * @since 2.3
	 * @author Christoph Strobl
	 */
	protected static class ConverterRegistrationIntent {

		private final ConverterRegistration delegate;
		private final ConverterOrigin origin;

		ConverterRegistrationIntent(ConverterRegistration delegate, ConverterOrigin origin) {
			this.delegate = delegate;
			this.origin = origin;
		}

		static ConverterRegistrationIntent userConverters(ConverterRegistration delegate) {
			return new ConverterRegistrationIntent(delegate, ConverterOrigin.USER_DEFINED);
		}

		static ConverterRegistrationIntent storeConverters(ConverterRegistration delegate) {
			return new ConverterRegistrationIntent(delegate, ConverterOrigin.STORE);
		}

		static ConverterRegistrationIntent defaultConverters(ConverterRegistration delegate) {
			return new ConverterRegistrationIntent(delegate, ConverterOrigin.DEFAULT);
		}

		Class<?> getSourceType() {
			return delegate.getConvertiblePair().getSourceType();
		}

		Class<?> getTargetType() {
			return delegate.getConvertiblePair().getTargetType();
		}

		public boolean isWriting() {
			return delegate.isWriting();
		}

		public boolean isReading() {
			return delegate.isReading();
		}

		public boolean isSimpleSourceType() {
			return delegate.isSimpleSourceType();
		}

		public boolean isSimpleTargetType() {
			return delegate.isSimpleTargetType();
		}

		public boolean isUserConverter() {
			return isConverterOfSource(ConverterOrigin.USER_DEFINED);
		}

		public boolean isStoreConverter() {
			return isConverterOfSource(ConverterOrigin.STORE);
		}

		public boolean isDefaultConverter() {
			return isConverterOfSource(ConverterOrigin.DEFAULT);
		}

		public ConverterRegistration getConverterRegistration() {
			return delegate;
		}

		private boolean isConverterOfSource(ConverterOrigin source) {
			return origin.equals(source);
		}

		protected enum ConverterOrigin {
			DEFAULT, USER_DEFINED, STORE
		}
	}

	/**
	 * Conversion registration information.
	 *
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	private static class ConverterRegistration {

		private final Object converter;
		private final GenericTypeDescriptorConverter.ConvertiblePair convertiblePair;
		private final StoreConversions storeConversions;
		private final boolean reading;
		private final boolean writing;

		private ConverterRegistration(Object converter, GenericTypeDescriptorConverter.ConvertiblePair convertiblePair, StoreConversions storeConversions,
                                      boolean reading, boolean writing) {
			this.converter = converter;
			this.convertiblePair = convertiblePair;
			this.storeConversions = storeConversions;
			this.reading = reading;
			this.writing = writing;
		}

		/**
		 * Returns whether the converter shall be used for writing.
		 *
		 * @return
		 */
		public boolean isWriting() {
			return writing || (!reading && isSimpleTargetType());
		}

		/**
		 * Returns whether the converter shall be used for reading.
		 *
		 * @return
		 */
		public boolean isReading() {
			return reading || (!writing && isSimpleSourceType());
		}

		/**
		 * Returns the actual conversion pair.
		 *
		 * @return
		 */
		public GenericTypeDescriptorConverter.ConvertiblePair getConvertiblePair() {
			return convertiblePair;
		}

		/**
		 * Returns whether the source type is a simple one.
		 *
		 * @return
		 */
		public boolean isSimpleSourceType() {
			return storeConversions.isStoreSimpleType(convertiblePair.getSourceType());
		}

		/**
		 * Returns whether the target type is a simple one.
		 *
		 * @return
		 */
		public boolean isSimpleTargetType() {
			return storeConversions.isStoreSimpleType(convertiblePair.getTargetType());
		}

		/**
		 * @return
		 */
		Object getConverter() {
			return converter;
		}
	}

	/**
	 * Value type to capture store-specific extensions to the {@link CustomConversions}. Allows to forward store specific
	 * default conversions and a set of types that are supposed to be considered simple.
	 *
	 * @author Oliver Gierke
	 */
	public static class StoreConversions {

		public static final StoreConversions NONE = StoreConversions.of(SimpleTypeHolder.DEFAULT, Collections.emptyList());

		private final SimpleTypeHolder storeTypeHolder;
		private final Collection<?> storeConverters;

		private StoreConversions(SimpleTypeHolder storeTypeHolder, Collection<?> storeConverters) {

			this.storeTypeHolder = storeTypeHolder;
			this.storeConverters = storeConverters;
		}

		/**
		 * Creates a new {@link StoreConversions} for the given store-specific {@link SimpleTypeHolder} and the given
		 * converters.
		 *
		 * @param storeTypeHolder must not be {@literal null}.
		 * @param converters must not be {@literal null}.
		 * @return
		 */
		public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Object... converters) {

			Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null!");
			Assert.notNull(converters, "Converters must not be null!");

			return new StoreConversions(storeTypeHolder, Arrays.asList(converters));
		}

		/**
		 * Creates a new {@link StoreConversions} for the given store-specific {@link SimpleTypeHolder} and the given
		 * converters.
		 *
		 * @param storeTypeHolder must not be {@literal null}.
		 * @param converters must not be {@literal null}.
		 * @return
		 */
		public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Collection<?> converters) {

			Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null!");
			Assert.notNull(converters, "Converters must not be null!");

			return new StoreConversions(storeTypeHolder, converters);
		}

		/**
		 * Returns {@link ConverterRegistration}s for the given converter.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public Streamable<ConverterRegistration> getRegistrationsFor(Object converter) {

			Assert.notNull(converter, "TypeConverter must not be null!");

			Class<?> type = converter.getClass();
			boolean isWriting = type.isAnnotationPresent(WritingConverter.class);
			boolean isReading = type.isAnnotationPresent(ReadingConverter.class);

			if (converter instanceof ConverterBuilder.ConverterAware) {

				return Streamable.of(() -> ConverterBuilder.ConverterAware.class.cast(converter).getConverters().stream()//
						.flatMap(it -> getRegistrationsFor(it).stream()));

			} else if (converter instanceof GenericTypeDescriptorConverter) {

				Set<GenericTypeDescriptorConverter.ConvertiblePair> convertibleTypes = GenericTypeDescriptorConverter.class.cast(converter).getConvertibleTypes();

				return convertibleTypes == null //
						? Streamable.empty() //
						: Streamable.of(convertibleTypes).map(it -> register(converter, it, isReading, isWriting));

			} else if (converter instanceof ConverterFactory) {

				return getRegistrationFor(converter, ConverterFactory.class, isReading, isWriting);

			} else if (converter instanceof TypeConverter) {

				return getRegistrationFor(converter, TypeConverter.class, isReading, isWriting);

			} else {
				throw new IllegalArgumentException(String.format("Unsupported converter type %s!", converter));
			}
		}

		private Streamable<ConverterRegistration> getRegistrationFor(Object converter, Class<?> type, boolean isReading,
				boolean isWriting) {

			Class<? extends Object> converterType = converter.getClass();
			Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(converterType, type);

			if (arguments == null) {
				throw new IllegalStateException(String.format("Couldn't resolve type arguments for %s!", converterType));
			}

			return Streamable.of(register(converter, arguments[0], arguments[1], isReading, isWriting));
		}

		private ConverterRegistration register(Object converter, Class<?> source, Class<?> target, boolean isReading,
				boolean isWriting) {
			return register(converter, new GenericTypeDescriptorConverter.ConvertiblePair(source, target), isReading, isWriting);
		}
		private ConverterRegistration register(Object converter, GenericTypeDescriptorConverter.ConvertiblePair pair, boolean isReading,
                                               boolean isWriting) {
			return new ConverterRegistration(converter, pair, this, isReading, isWriting);
		}

		private boolean isStoreSimpleType(Class<?> type) {
			return storeTypeHolder.isSimpleType(type);
		}

		SimpleTypeHolder getStoreTypeHolder() {
			return this.storeTypeHolder;
		}

		Collection<?> getStoreConverters() {
			return this.storeConverters;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof StoreConversions)) {
				return false;
			}

			StoreConversions that = (StoreConversions) o;
			if (!ObjectUtils.nullSafeEquals(storeTypeHolder, that.storeTypeHolder)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(storeConverters, that.storeConverters);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(storeTypeHolder);
			result = 31 * result + ObjectUtils.nullSafeHashCode(storeConverters);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "StoreConversions{" + "storeTypeHolder=" + storeTypeHolder + ", storeConverters=" + storeConverters + '}';
		}
	}

	/**
	 * Value object holding the actual {@link StoreConversions} and custom {@link TypeConverter converters} configured for
	 * registration.
	 *
	 * @author Christoph Strobl
	 * @since 2.3
	 */
	protected static class ConverterConfiguration {

		private final StoreConversions storeConversions;
		private final List<?> userConverters;
		private final Predicate<GenericTypeDescriptorConverter.ConvertiblePair> converterRegistrationFilter;

		/**
		 * Create a new ConverterConfiguration holding the given {@link StoreConversions} and user defined converters.
		 *
		 * @param storeConversions must not be {@literal null}.
		 * @param userConverters must not be {@literal null} use {@link Collections#emptyList()} instead.
		 */
		public ConverterConfiguration(StoreConversions storeConversions, List<?> userConverters) {
			this(storeConversions, userConverters, it -> true);
		}

		/**
		 * Create a new ConverterConfiguration holding the given {@link StoreConversions} and user defined converters as
		 * well as a {@link Collection} of {@link ConvertiblePair} for which to skip the registration of default converters.
		 * <br />
		 * This allows store implementations to modify default converter registration based on specific needs and
		 * configurations. User defined converters will are never subject of filtering.
		 *
		 * @param storeConversions must not be {@literal null}.
		 * @param userConverters must not be {@literal null} use {@link Collections#emptyList()} instead.
		 * @param converterRegistrationFilter must not be {@literal null}..
		 */
		public ConverterConfiguration(StoreConversions storeConversions, List<?> userConverters,
				Predicate<GenericTypeDescriptorConverter.ConvertiblePair> converterRegistrationFilter) {

			this.storeConversions = storeConversions;
			this.userConverters = new ArrayList<>(userConverters);
			this.converterRegistrationFilter = converterRegistrationFilter;
		}

		/**
		 * @return never {@literal null}
		 */
		StoreConversions getStoreConversions() {
			return storeConversions;
		}

		/**
		 * @return never {@literal null}.
		 */
		List<?> getUserConverters() {
			return userConverters;
		}

		/**
		 * @return never {@literal null}.
		 */
		boolean shouldRegister(GenericTypeDescriptorConverter.ConvertiblePair candidate) {
			return this.converterRegistrationFilter.test(candidate);
		}
	}
}
