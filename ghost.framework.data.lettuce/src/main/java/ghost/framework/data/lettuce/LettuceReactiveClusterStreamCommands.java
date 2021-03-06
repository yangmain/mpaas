/*
 * Copyright 2018-2020 the original author or authors.
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
package ghost.framework.data.lettuce;

import ghost.framework.data.redis.connection.ReactiveClusterStreamCommands;

/**
 * @author Mark Paluch
 * @since 2.2
 */
class LettuceReactiveClusterStreamCommands extends LettuceReactiveStreamCommands
		implements ReactiveClusterStreamCommands {

	/**
	 * Create new {@link LettuceReactiveClusterStreamCommands}.
	 *
	 * @param connection must not be {@literal null}.
	 */
	LettuceReactiveClusterStreamCommands(LettuceReactiveRedisConnection connection) {
		super(connection);
	}
}
