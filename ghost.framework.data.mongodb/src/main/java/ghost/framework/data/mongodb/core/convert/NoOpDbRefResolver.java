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
package ghost.framework.data.mongodb.core.convert;

import com.mongodb.DBRef;
import org.bson.Document;
import ghost.framework.data.mongodb.core.mapping.MongoPersistentProperty;
import ghost.framework.beans.annotation.constraints.Nullable;

import java.util.List;

/**
 * No-Operation {@link ghost.framework.data.mongodb.core.mapping.DBRef} resolver throwing
 * {@link UnsupportedOperationException} when attempting to resolve database references.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.1
 */
public enum NoOpDbRefResolver implements DbRefResolver {

	INSTANCE;

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.mongodb.core.convert.DbRefResolver#resolveDbRef(ghost.framework.data.mongodb.core.mapping.MongoPersistentProperty, ghost.framework.data.mongodb.core.convert.DbRefResolverCallback, ghost.framework.data.mongodb.core.convert.DbRefProxyHandler)
	 */
	@Override
	@Nullable
	public Object resolveDbRef(MongoPersistentProperty property, @Nullable DBRef dbref, DbRefResolverCallback callback,
			DbRefProxyHandler proxyHandler) {

		return handle();
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.mongodb.core.convert.DbRefResolver#fetch(com.mongodb.DBRef)
	 */
	@Override
	@Nullable
	public Document fetch(DBRef dbRef) {
		return handle();
	}

	/*
	 * (non-Javadoc)
	 * @see ghost.framework.data.mongodb.core.convert.DbRefResolver#bulkFetch(java.util.List)
	 */
	@Override
	public List<Document> bulkFetch(List<DBRef> dbRefs) {
		return handle();
	}

	private <T> T handle() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("DBRef resolution is not supported!");
	}
}
