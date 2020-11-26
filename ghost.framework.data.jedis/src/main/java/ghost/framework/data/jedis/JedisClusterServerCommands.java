/*
 * Copyright 2017-2020 the original author or authors.
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
package ghost.framework.data.jedis;

import ghost.framework.beans.annotation.constraints.Nullable;
import ghost.framework.data.dao.InvalidDataAccessApiUsageException;
import ghost.framework.data.redis.connection.ClusterCommandExecutor;
import ghost.framework.data.redis.connection.RedisClusterNode;
import ghost.framework.data.redis.connection.RedisClusterServerCommands;
import ghost.framework.data.redis.connection.RedisNode;
import ghost.framework.data.redis.connection.convert.Converters;
import ghost.framework.data.redis.connection.core.types.RedisClientInfo;
import ghost.framework.util.Assert;
import ghost.framework.util.CollectionUtils;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author Mark Paluch
 * @since 2.0
 */
class JedisClusterServerCommands implements RedisClusterServerCommands {

	private final JedisClusterConnection connection;

	JedisClusterServerCommands(JedisClusterConnection connection) {
		this.connection = connection;
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#bgReWriteAof(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public void bgReWriteAof(RedisClusterNode node) {
		executeCommandOnSingleNode(BinaryJedis::bgrewriteaof, node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#bgReWriteAof()
	 */
	@Override
	public void bgReWriteAof() {
		connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) BinaryJedis::bgrewriteaof);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#bgSave()
	 */
	@Override
	public void bgSave() {
		connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) BinaryJedis::bgsave);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#bgSave(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public void bgSave(RedisClusterNode node) {
		executeCommandOnSingleNode(BinaryJedis::bgsave, node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#lastSave()
	 */
	@Override
	public Long lastSave() {

		List<Long> result = new ArrayList<>(executeCommandOnAllNodes(BinaryJedis::lastsave).resultsAsList());

		if (CollectionUtils.isEmpty(result)) {
			return null;
		}

		Collections.sort(result, Collections.reverseOrder());
		return result.get(0);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#lastSave(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public Long lastSave(RedisClusterNode node) {
		return executeCommandOnSingleNode(BinaryJedis::lastsave, node).getValue();
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#save()
	 */
	@Override
	public void save() {
		executeCommandOnAllNodes(BinaryJedis::save);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#save(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public void save(RedisClusterNode node) {
		executeCommandOnSingleNode(BinaryJedis::save, node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#dbSize()
	 */
	@Override
	public Long dbSize() {

		Collection<Long> dbSizes = executeCommandOnAllNodes(BinaryJedis::dbSize).resultsAsList();

		if (CollectionUtils.isEmpty(dbSizes)) {
			return 0L;
		}

		Long size = 0L;
		for (Long value : dbSizes) {
			size += value;
		}
		return size;
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#dbSize(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public Long dbSize(RedisClusterNode node) {
		return executeCommandOnSingleNode(BinaryJedis::dbSize, node).getValue();
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#flushDb()
	 */
	@Override
	public void flushDb() {
		executeCommandOnAllNodes(BinaryJedis::flushDB);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#flushDb(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public void flushDb(RedisClusterNode node) {
		executeCommandOnSingleNode(BinaryJedis::flushDB, node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#flushAll()
	 */
	@Override
	public void flushAll() {
		connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) BinaryJedis::flushAll);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#flushAll(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public void flushAll(RedisClusterNode node) {
		executeCommandOnSingleNode(BinaryJedis::flushAll, node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#info()
	 */
	@Override
	public Properties info() {

		Properties infos = new Properties();

		List<ClusterCommandExecutor.NodeResult<Properties>> nodeResults = connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes(
						(JedisClusterConnection.JedisClusterCommandCallback<Properties>) client -> JedisConverters.toProperties(client.info()))
				.getResults();

		for (ClusterCommandExecutor.NodeResult<Properties> nodeProperties : nodeResults) {
			for (Entry<Object, Object> entry : nodeProperties.getValue().entrySet()) {
				infos.put(nodeProperties.getNode().asString() + "." + entry.getKey(), entry.getValue());
			}
		}

		return infos;
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#info(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public Properties info(RedisClusterNode node) {
		return JedisConverters.toProperties(executeCommandOnSingleNode(BinaryJedis::info, node).getValue());
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#info(java.lang.String)
	 */
	@Override
	public Properties info(String section) {

		Assert.notNull(section, "Section must not be null!");
		Properties infos = new Properties();

		List<ClusterCommandExecutor.NodeResult<Properties>> nodeResults = connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes(
						(JedisClusterConnection.JedisClusterCommandCallback<Properties>) client -> JedisConverters.toProperties(client.info(section)))
				.getResults();

		for (ClusterCommandExecutor.NodeResult<Properties> nodeProperties : nodeResults) {
			for (Entry<Object, Object> entry : nodeProperties.getValue().entrySet()) {
				infos.put(nodeProperties.getNode().asString() + "." + entry.getKey(), entry.getValue());
			}
		}

		return infos;
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#info(ghost.framework.data.redis.connection.RedisClusterNode, java.lang.String)
	 */
	@Override
	public Properties info(RedisClusterNode node, String section) {

		Assert.notNull(section, "Section must not be null!");

		return JedisConverters.toProperties(executeCommandOnSingleNode(client -> client.info(section), node).getValue());
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#shutdown()
	 */
	@Override
	public void shutdown() {
		connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) BinaryJedis::shutdown);
	}
	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#shutdown(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public void shutdown(RedisClusterNode node) {
		executeCommandOnSingleNode(BinaryJedis::shutdown, node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#shutdown(ghost.framework.data.redis.connection.RedisServerCommands.ShutdownOption)
	 */
	@Override
	public void shutdown(ShutdownOption option) {

		if (option == null) {
			shutdown();
			return;
		}

		throw new IllegalArgumentException("Shutdown with options is not supported for jedis.");
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#getConfig(java.lang.String)
	 */
	@Override
	public Properties getConfig(String pattern) {
		Assert.notNull(pattern, "Pattern must not be null!");
		List<ClusterCommandExecutor.NodeResult<List<String>>> mapResult = connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<List<String>>) client -> client.configGet(pattern))
				.getResults();

		List<String> result = new ArrayList<>();
		for (ClusterCommandExecutor.NodeResult<List<String>> entry : mapResult) {

			String prefix = entry.getNode().asString();
			int i = 0;
			for (String value : entry.getValue()) {
				result.add((i++ % 2 == 0 ? (prefix + ".") : "") + value);
			}
		}

		return Converters.toProperties(result);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#getConfig(ghost.framework.data.redis.connection.RedisClusterNode, java.lang.String)
	 */
	@Override
	public Properties getConfig(RedisClusterNode node, String pattern) {

		Assert.notNull(pattern, "Pattern must not be null!");

		return connection.getClusterCommandExecutor()
				.executeCommandOnSingleNode(
						(JedisClusterConnection.JedisClusterCommandCallback<Properties>) client -> Converters.toProperties(client.configGet(pattern)),
						node)
				.getValue();
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#setConfig(java.lang.String, java.lang.String)
	 */
	@Override
	public void setConfig(String param, String value) {

		Assert.notNull(param, "Parameter must not be null!");
		Assert.notNull(value, "Value must not be null!");
		connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) client -> client.configSet(param, value));
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#setConfig(ghost.framework.data.redis.connection.RedisClusterNode, java.lang.String, java.lang.String)
	 */
	@Override
	public void setConfig(RedisClusterNode node, String param, String value) {

		Assert.notNull(param, "Parameter must not be null!");
		Assert.notNull(value, "Value must not be null!");

		executeCommandOnSingleNode(client -> client.configSet(param, value), node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#resetConfigStats()
	 */
	@Override
	public void resetConfigStats() {
		connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) BinaryJedis::configResetStat);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#resetConfigStats(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public void resetConfigStats(RedisClusterNode node) {
		executeCommandOnSingleNode(BinaryJedis::configResetStat, node);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#time()
	 */
	@Override
	public Long time() {
		return convertListOfStringToTime(connection.getClusterCommandExecutor()
				.executeCommandOnArbitraryNode((JedisClusterConnection.JedisClusterCommandCallback<List<String>>) BinaryJedis::time).getValue());
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#time(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public Long time(RedisClusterNode node) {

		return convertListOfStringToTime(connection.getClusterCommandExecutor()
				.executeCommandOnSingleNode((JedisClusterConnection.JedisClusterCommandCallback<List<String>>) BinaryJedis::time, node).getValue());
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#killClient(java.lang.String, int)
	 */
	@Override
	public void killClient(String host, int port) {

		Assert.hasText(host, "Host for 'CLIENT KILL' must not be 'null' or 'empty'.");
		String hostAndPort = String.format("%s:%s", host, port);

		connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) client -> client.clientKill(hostAndPort));
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#setClientName(byte[])
	 */
	@Override
	public void setClientName(byte[] name) {
		throw new InvalidDataAccessApiUsageException("CLIENT SETNAME is not supported in cluster environment.");
	}
	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#getClientName()
	 */
	@Override
	public String getClientName() {
		throw new InvalidDataAccessApiUsageException("CLIENT GETNAME is not supported in cluster environment.");
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#getClientList()
	 */
	@Override
	public List<RedisClientInfo> getClientList() {

		Collection<String> map = connection.getClusterCommandExecutor()
				.executeCommandOnAllNodes((JedisClusterConnection.JedisClusterCommandCallback<String>) Jedis::clientList).resultsAsList();

		ArrayList<RedisClientInfo> result = new ArrayList<>();
		for (String infos : map) {
			result.addAll(JedisConverters.toListOfRedisClientInformation(infos));
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisClusterServerCommands#getClientList(ghost.framework.data.redis.connection.RedisClusterNode)
	 */
	@Override
	public List<RedisClientInfo> getClientList(RedisClusterNode node) {

		return JedisConverters
				.toListOfRedisClientInformation(executeCommandOnSingleNode(Jedis::clientList, node).getValue());
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#slaveOf(java.lang.String, int)
	 */
	@Override
	public void slaveOf(String host, int port) {
		throw new InvalidDataAccessApiUsageException(
				"SlaveOf is not supported in cluster environment. Please use CLUSTER REPLICATE.");
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#slaveOfNoOne()
	 */
	@Override
	public void slaveOfNoOne() {
		throw new InvalidDataAccessApiUsageException(
				"SlaveOf is not supported in cluster environment. Please use CLUSTER REPLICATE.");
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#migrate(byte[], ghost.framework.data.redis.connection.RedisNode, int, ghost.framework.data.redis.connection.RedisServerCommands.MigrateOption)
	 */
	@Override
	public void migrate(byte[] key, RedisNode target, int dbIndex, @Nullable MigrateOption option) {
		migrate(key, target, dbIndex, option, Long.MAX_VALUE);
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.redis.connection.RedisServerCommands#migrate(byte[], ghost.framework.data.redis.connection.RedisNode, int, ghost.framework.data.redis.connection.RedisServerCommands.MigrateOption, long)
	 */
	@Override
	public void migrate(byte[] key, RedisNode target, int dbIndex, @Nullable MigrateOption option, long timeout) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(target, "Target node must not be null!");
		int timeoutToUse = timeout <= Integer.MAX_VALUE ? (int) timeout : Integer.MAX_VALUE;

		RedisClusterNode node = connection.getTopologyProvider().getTopology().lookup(target.getHost(), target.getPort());

		executeCommandOnSingleNode(client -> client.migrate(target.getHost(), target.getPort(), key, dbIndex, timeoutToUse),
				node);
	}

	private Long convertListOfStringToTime(List<String> serverTimeInformation) {

		Assert.notEmpty(serverTimeInformation, "Received invalid result from server. Expected 2 items in collection.");
		Assert.isTrue(serverTimeInformation.size() == 2,
				"Received invalid number of arguments from redis server. Expected 2 received " + serverTimeInformation.size());

		return Converters.toTimeMillis(serverTimeInformation.get(0), serverTimeInformation.get(1));
	}

	private <T> ClusterCommandExecutor.NodeResult<T> executeCommandOnSingleNode(JedisClusterConnection.JedisClusterCommandCallback<T> cmd, RedisClusterNode node) {
		return connection.getClusterCommandExecutor().executeCommandOnSingleNode(cmd, node);
	}

	private <T> ClusterCommandExecutor.MultiNodeResult<T> executeCommandOnAllNodes(JedisClusterConnection.JedisClusterCommandCallback<T> cmd) {
		return connection.getClusterCommandExecutor().executeCommandOnAllNodes(cmd);
	}
}