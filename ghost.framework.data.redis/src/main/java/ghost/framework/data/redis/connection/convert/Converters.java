/*
 * Copyright 2013-2020 the original author or authors.
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
package ghost.framework.data.redis.connection.convert;

import ghost.framework.beans.annotation.application.Application;
import ghost.framework.beans.annotation.constraints.Nullable;
import ghost.framework.beans.annotation.injection.Autowired;
import ghost.framework.context.converter.Converter;
import ghost.framework.context.converter.primitive.LongConverterBoolean;
import ghost.framework.context.converter.properties.MapConverterProperties;
import ghost.framework.context.converter.properties.StringConverterProperties;
import ghost.framework.context.converter.properties.StringListConverterProperties;
import ghost.framework.data.geo.*;
import ghost.framework.data.redis.connection.*;
import ghost.framework.data.redis.serializer.RedisSerializer;
import ghost.framework.data.redis.util.ByteUtils;
import ghost.framework.util.*;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Common type converters
 *
 * @author Jennifer Hickey
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 */
abstract public class Converters {
	private static final Converter<String, byte[]> STRING_TO_BYTES;

	public static byte[] toBytes(String source) {
		return STRING_TO_BYTES.convert(source);
	}
	// private static final Log LOGGER = LogFactory.getLog(Converters.class);
	private static final Log LOGGER = LogFactory.getLog(Converters.class);

	private static final byte[] ONE = new byte[] { '1' };
	private static final byte[] ZERO = new byte[] { '0' };
	private static final String CLUSTER_NODES_LINE_SEPARATOR = "\n";
	@Autowired
	@Application
	private static StringConverterProperties STRING_TO_PROPS;// = new DefaultStringToPropertiesConverter();
	@Autowired
	@Application
	private static LongConverterBoolean<Long, Boolean> LONG_TO_BOOLEAN;// = new DefaultLongToBooleanConverter();
	private static final Converter<String, DataType> STRING_TO_DATA_TYPE = new StringToDataTypeConverter();
	@Autowired
	@Application
	private static MapConverterProperties<Map<?, ?>, Properties> MAP_TO_PROPERTIES;// = MapToPropertiesConverter.INSTANCE;
	private static final Converter<String, RedisClusterNode> STRING_TO_CLUSTER_NODE_CONVERTER;
	@Autowired
	@Application
	private static StringListConverterProperties STRING_LIST_TO_PROPERTIES_CONVERTER;
	private static final Map<String, RedisClusterNode.Flag> flagLookupMap;
	public static byte[] toBytes(Integer source) {
		return String.valueOf(source).getBytes();
	}

	public static byte[] toBytes(Long source) {
		return String.valueOf(source).getBytes();
	}

	/**
	 * @param source
	 * @return
	 * @since 1.6
	 */
	public static byte[] toBytes(Double source) {
		return toBytes(String.valueOf(source));
	}
	static {
		STRING_TO_BYTES = source -> {
			if (source == null) {
				return null;
			}
			return source.getBytes();
		};
		flagLookupMap = new LinkedHashMap<>(RedisClusterNode.Flag.values().length, 1);
		for (RedisClusterNode.Flag flag : RedisClusterNode.Flag.values()) {
			flagLookupMap.put(flag.getRaw(), flag);
		}

		STRING_TO_CLUSTER_NODE_CONVERTER = new Converter<String, RedisClusterNode>() {

			static final int ID_INDEX = 0;
			static final int HOST_PORT_INDEX = 1;
			static final int FLAGS_INDEX = 2;
			static final int MASTER_ID_INDEX = 3;
			static final int LINK_STATE_INDEX = 7;
			static final int SLOTS_INDEX = 8;

			@Override
			public RedisClusterNode convert(String source) {

				String[] args = source.split(" ");
				String[] hostAndPort = StringUtils.split(args[HOST_PORT_INDEX], ":");

				Assert.notNull(hostAndPort, "CusterNode information does not define host and port!");

				RedisClusterNode.SlotRange range = parseSlotRange(args);
				Set<RedisClusterNode.Flag> flags = parseFlags(args);

				String portPart = hostAndPort[1];
				if (portPart.contains("@")) {
					portPart = portPart.substring(0, portPart.indexOf('@'));
				}

				RedisClusterNode.RedisClusterNodeBuilder nodeBuilder = RedisClusterNode.newRedisClusterNode()
						.listeningAt(hostAndPort[0], Integer.valueOf(portPart)) //
						.withId(args[ID_INDEX]) //
						.promotedAs(flags.contains(RedisClusterNode.Flag.MASTER) ? RedisNode.NodeType.MASTER : RedisNode.NodeType.SLAVE) //
						.serving(range) //
						.withFlags(flags) //
						.linkState(parseLinkState(args));

				if (!args[MASTER_ID_INDEX].isEmpty() && !args[MASTER_ID_INDEX].startsWith("-")) {
					nodeBuilder.slaveOf(args[MASTER_ID_INDEX]);
				}

				return nodeBuilder.build();
			}

			private Set<RedisClusterNode.Flag> parseFlags(String[] args) {

				String raw = args[FLAGS_INDEX];

				Set<RedisClusterNode.Flag> flags = new LinkedHashSet<>(8, 1);
				if (StringUtils.hasText(raw)) {
					for (String flag : raw.split(",")) {
						flags.add(flagLookupMap.get(flag));
					}
				}
				return flags;
			}

			private RedisClusterNode.LinkState parseLinkState(String[] args) {

				String raw = args[LINK_STATE_INDEX];

				if (StringUtils.hasText(raw)) {
					return RedisClusterNode.LinkState.valueOf(raw.toUpperCase());
				}
				return RedisClusterNode.LinkState.DISCONNECTED;
			}

			private RedisClusterNode.SlotRange parseSlotRange(String[] args) {

				Set<Integer> slots = new LinkedHashSet<>();

				for (int i = SLOTS_INDEX; i < args.length; i++) {

					String raw = args[i];

					if (raw.startsWith("[")) {
						continue;
					}

					if (raw.contains("-")) {
						String[] slotRange = StringUtils.split(raw, "-");

						if (slotRange != null) {
							int from = Integer.valueOf(slotRange[0]);
							int to = Integer.valueOf(slotRange[1]);
							for (int slot = from; slot <= to; slot++) {
								slots.add(slot);
							}
						}
					} else {
						slots.add(Integer.valueOf(raw));
					}
				}

				return new RedisClusterNode.SlotRange(slots);
			}

		};

//		STRING_LIST_TO_PROPERTIES_CONVERTER = input -> {
//
//			Assert.notNull(input, "Input list must not be null!");
//			Assert.isTrue(input.size() % 2 == 0, "Input list must contain an even number of entries!");
//
//			Properties properties = new Properties();
//
//			for (int i = 0; i < input.size(); i += 2) {
//
//				properties.setProperty(input.get(i), input.get(i + 1));
//			}
//
//			return properties;
//		};
	}

	public static Boolean stringToBoolean(String s) {
		return stringToBooleanConverter().convert(s);
	}

	public static Converter<String, Boolean> stringToBooleanConverter() {
		return (source) -> ObjectUtils.nullSafeEquals("OK", source);
	}

	public static Converter<String, Properties> stringToProps() {
		return STRING_TO_PROPS;
	}

	public static Converter<Long, Boolean> longToBoolean() {
		return LONG_TO_BOOLEAN;
	}

	public static Converter<String, DataType> stringToDataType() {
		return STRING_TO_DATA_TYPE;
	}

	public static Properties toProperties(String source) {
		return STRING_TO_PROPS.convert(source);
	}

	public static Properties toProperties(Map<?, ?> source) {

		Properties properties = MAP_TO_PROPERTIES.convert(source);
		return properties != null ? properties : new Properties();
	}

	public static Boolean toBoolean(Long source) {
		return LONG_TO_BOOLEAN.convert(source);
	}

	public static DataType toDataType(String source) {
		return STRING_TO_DATA_TYPE.convert(source);
	}

	public static byte[] toBit(Boolean source) {
		return (source ? ONE : ZERO);
	}

	/**
	 * Converts the result of a single line of {@code CLUSTER NODES} into a {@link RedisClusterNode}.
	 *
	 * @param clusterNodesLine
	 * @return
	 * @since 1.7
	 */
	protected static RedisClusterNode toClusterNode(String clusterNodesLine) {
		return STRING_TO_CLUSTER_NODE_CONVERTER.convert(clusterNodesLine);
	}

	/**
	 * Converts lines from the result of {@code CLUSTER NODES} into {@link RedisClusterNode}s.
	 *
	 * @param lines
	 * @return
	 * @since 1.7
	 */
	public static Set<RedisClusterNode> toSetOfRedisClusterNodes(Collection<String> lines) {

		if (CollectionUtils.isEmpty(lines)) {
			return Collections.emptySet();
		}

		Set<RedisClusterNode> nodes = new LinkedHashSet<>(lines.size());

		for (String line : lines) {
			nodes.add(toClusterNode(line));
		}

		return nodes;
	}

	/**
	 * Converts the result of {@code CLUSTER NODES} into {@link RedisClusterNode}s.
	 *
	 * @param clusterNodes
	 * @return
	 * @since 1.7
	 */
	public static Set<RedisClusterNode> toSetOfRedisClusterNodes(String clusterNodes) {

		if (StringUtils.isEmpty(clusterNodes)) {
			return Collections.emptySet();
		}

		String[] lines = clusterNodes.split(CLUSTER_NODES_LINE_SEPARATOR);
		return toSetOfRedisClusterNodes(Arrays.asList(lines));
	}

	public static List<Object> toObjects(Set<RedisZSetCommands.Tuple> tuples) {
		List<Object> tupleArgs = new ArrayList<>(tuples.size() * 2);
		for (RedisZSetCommands.Tuple tuple : tuples) {
			tupleArgs.add(tuple.getScore());
			tupleArgs.add(tuple.getValue());
		}
		return tupleArgs;
	}

	/**
	 * Returns the timestamp constructed from the given {@code seconds} and {@code microseconds}.
	 *
	 * @param seconds server time in seconds
	 * @param microseconds elapsed microseconds in current second
	 * @return
	 */
	public static Long toTimeMillis(String seconds, String microseconds) {
		return NumberUtils.parseNumber(seconds, Long.class) * 1000L
				+ NumberUtils.parseNumber(microseconds, Long.class) / 1000L;
	}

	/**
	 * Converts {@code seconds} to the given {@link TimeUnit}.
	 *
	 * @param seconds
	 * @param targetUnit must not be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	public static long secondsToTimeUnit(long seconds, TimeUnit targetUnit) {

		Assert.notNull(targetUnit, "TimeUnit must not be null!");

		if (seconds > 0) {
			return targetUnit.convert(seconds, TimeUnit.SECONDS);
		}

		return seconds;
	}

	/**
	 * Creates a new {@link Converter} to convert from seconds to the given {@link TimeUnit}.
	 *
	 * @param timeUnit muist not be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	public static Converter<Long, Long> secondsToTimeUnit(final TimeUnit timeUnit) {

		return seconds -> secondsToTimeUnit(seconds, timeUnit);
	}

	/**
	 * Converts {@code milliseconds} to the given {@link TimeUnit}.
	 *
	 * @param milliseconds
	 * @param targetUnit must not be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	public static long millisecondsToTimeUnit(long milliseconds, TimeUnit targetUnit) {

		Assert.notNull(targetUnit, "TimeUnit must not be null!");

		if (milliseconds > 0) {
			return targetUnit.convert(milliseconds, TimeUnit.MILLISECONDS);
		}

		return milliseconds;
	}

	/**
	 * Creates a new {@link Converter} to convert from milliseconds to the given {@link TimeUnit}.
	 *
	 * @param timeUnit muist not be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	public static Converter<Long, Long> millisecondsToTimeUnit(final TimeUnit timeUnit) {

		return seconds -> millisecondsToTimeUnit(seconds, timeUnit);
	}

	/**
	 * {@link Converter} capable of deserializing {@link GeoResults}.
	 *
	 * @param serializer
	 * @return
	 * @since 1.8
	 */
	public static <V> Converter<GeoResults<RedisGeoCommands.GeoLocation<byte[]>>, GeoResults<RedisGeoCommands.GeoLocation<V>>> deserializingGeoResultsConverter(
			RedisSerializer<V> serializer) {
		return new DeserializingGeoResultsConverter<>(serializer);
	}

	/**
	 * {@link Converter} capable of converting Double into {@link Distance} using given {@link Metric}.
	 *
	 * @param metric
	 * @return
	 * @since 1.8
	 */
	public static Converter<Double, Distance> distanceConverterForMetric(Metric metric) {
		return DistanceConverterFactory.INSTANCE.forMetric(metric);
	}

	/**
	 * Converts array outputs with key-value sequences (such as produced by {@code CONFIG GET}) from a {@link List} to
	 * {@link Properties}.
	 *
	 * @param input must not be {@literal null}.
	 * @return the mapped result.
	 * @since 2.0
	 */
	public static Properties toProperties(List<String> input) {
		return STRING_LIST_TO_PROPERTIES_CONVERTER.convert(input);
	}

	/**
	 * Returns a converter to convert array outputs with key-value sequences (such as produced by {@code CONFIG GET}) from
	 * a {@link List} to {@link Properties}.
	 *
	 * @return the converter.
	 * @since 2.0
	 */
	public static Converter<List<String>, Properties> listToPropertiesConverter() {
		return STRING_LIST_TO_PROPERTIES_CONVERTER;
	}

	/**
	 * Returns a converter to convert from {@link Map} to {@link Properties}.
	 *
	 * @return the converter.
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Converter<Map<K, V>, Properties> mapToPropertiesConverter() {
		return (Converter) MAP_TO_PROPERTIES;
	}

	/**
	 * Convert the given {@literal nullable seconds} to a {@link Duration} or {@literal null}.
	 *
	 * @param seconds can be {@literal null}.
	 * @return given {@literal seconds} as {@link Duration} or {@literal null}.
	 * @since 2.1
	 */
	@Nullable
	public static Duration secondsToDuration(@Nullable Long seconds) {
		return seconds != null ? Duration.ofSeconds(seconds) : null;
	}

	/**
	 * Parse a rather generic Redis response, such as a list of something into a meaningful structure applying best effort
	 * conversion of {@code byte[]} and {@link ByteBuffer}.
	 * 
	 * @param source the source to parse
	 * @param targetType eg. {@link Map}, {@link String},...
	 * @param <T>
	 * @return
	 * @since 2.3
	 */
	public static <T> T parse(Object source, Class<T> targetType) {
		return targetType.cast(parse(source, "root", Collections.singletonMap("root", targetType)));
	}

	/**
	 * Parse a rather generic Redis response, such as a list of something into a meaningful structure applying best effort
	 * conversion of {@code byte[]} and {@link ByteBuffer} based on the {@literal sourcePath} and a {@literal typeHintMap}
	 *
	 * @param source the source to parse
	 * @param sourcePath the current path (use "root", for level 0).
	 * @param typeHintMap source path to target type hints allowing wildcards ({@literal *}).
	 * @return
	 * @since 2.3
	 */
	public static Object parse(Object source, String sourcePath, Map<String, Class<?>> typeHintMap) {

		String path = sourcePath;
		Class<?> targetType = typeHintMap.get(path);

		if (targetType == null) {

			String alternatePath = sourcePath.contains(".") ? sourcePath.substring(0, sourcePath.lastIndexOf(".")) + ".*"
					: sourcePath;
			targetType = typeHintMap.get(alternatePath);
			if (targetType == null) {

				if (sourcePath.endsWith("[]")) {
					targetType = String.class;
				} else {
					targetType = source.getClass();
				}

			} else {
				if (targetType == Map.class && sourcePath.endsWith("[]")) {
					targetType = String.class;
				} else {
					path = alternatePath;
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("parsing %s (%s) as %s.", sourcePath, path, targetType));
		}

		if (targetType == Object.class) {
			return source;
		}

		if (ClassUtils.isAssignable(String.class, targetType)) {

			if (source instanceof String) {
				return source.toString();
			}
			if (source instanceof byte[]) {
				return new String((byte[]) source);
			}
			if (source instanceof ByteBuffer) {
				return new String(ByteUtils.getBytes((ByteBuffer) source));
			}
		}
		if (ClassUtils.isAssignable(List.class, targetType) && source instanceof List) {

			List<Object> sourceCollection = (List<Object>) source;
			List<Object> targetList = new ArrayList<>();
			for (int i = 0; i < sourceCollection.size(); i++) {
				targetList.add(parse(sourceCollection.get(i), sourcePath + ".[" + i + "]", typeHintMap));
			}
			return targetList;
		}

		if (ClassUtils.isAssignable(Map.class, targetType) && source instanceof List) {

			List<Object> sourceCollection = ((List<Object>) source);
			Map<String, Object> targetMap = new LinkedHashMap<>();
			for (int i = 0; i < sourceCollection.size(); i = i + 2) {

				String key = parse(sourceCollection.get(i), path + ".[]", typeHintMap).toString();
				targetMap.put(key, parse(sourceCollection.get(i + 1), path + "." + key, typeHintMap));
			}
			return targetMap;
		}

		return source;
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.8
	 */
	enum DistanceConverterFactory {

		INSTANCE;

		/**
		 * @param metric can be {@literal null}. Defaults to {@link RedisGeoCommands.DistanceUnit#METERS}.
		 * @return never {@literal null}.
		 */
		DistanceConverter forMetric(@Nullable Metric metric) {
			return new DistanceConverter(
					metric == null || ObjectUtils.nullSafeEquals(Metrics.NEUTRAL, metric) ? RedisGeoCommands.DistanceUnit.METERS : metric);
		}

		static class DistanceConverter implements Converter<Double, Distance> {

			private Metric metric;

			/**
			 * @param metric can be {@literal null}. Defaults to {@link RedisGeoCommands.DistanceUnit#METERS}.
			 * @return never {@literal null}.
			 */
			DistanceConverter(@Nullable Metric metric) {
				this.metric = metric == null || ObjectUtils.nullSafeEquals(Metrics.NEUTRAL, metric) ? RedisGeoCommands.DistanceUnit.METERS
						: metric;
			}

			/*
			 * (non-Javadoc)
			 * @see ghost.framework.core.convert.converter.Converter#convert(Object)
			 */
			@Override
			public Distance convert(Double source) {
				return new Distance(source, metric);
			}
		}
	}

	/**
	 * @author Christoph Strobl
	 * @param <V>
	 * @since 1.8
	 */
	static class DeserializingGeoResultsConverter<V>
			implements Converter<GeoResults<RedisGeoCommands.GeoLocation<byte[]>>, GeoResults<RedisGeoCommands.GeoLocation<V>>> {

		final RedisSerializer<V> serializer;

		public DeserializingGeoResultsConverter(RedisSerializer<V> serializer) {
			this.serializer = serializer;
		}

		/*
		 * (non-Javadoc)
		 * @see ghost.framework.core.convert.converter.Converter#convert(Object)
		 */
		@Override
		public GeoResults<RedisGeoCommands.GeoLocation<V>> convert(GeoResults<RedisGeoCommands.GeoLocation<byte[]>> source) {

			List<GeoResult<RedisGeoCommands.GeoLocation<V>>> values = new ArrayList<>(source.getContent().size());
			for (GeoResult<RedisGeoCommands.GeoLocation<byte[]>> value : source.getContent()) {

				values.add(new GeoResult<>(
						new RedisGeoCommands.GeoLocation<>(serializer.deserialize(value.getContent().getName()), value.getContent().getPoint()),
						value.getDistance()));
			}

			return new GeoResults<>(values, source.getAverageDistance().getMetric());
		}
	}
}
