/*
 * Copyright 2002-2017 the original author or authors.
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

package ghost.framework.data.jdbc.support;

import ghost.framework.beans.annotation.constraints.Nullable;
import ghost.framework.data.dao.*;

import java.sql.*;

/**
 * {@link SQLExceptionTranslator} implementation which analyzes the specific
 * {@link SQLException} subclass thrown by the JDBC driver.
 *
 * <p>Falls back to a standard {@link SQLStateSQLExceptionTranslator} if the JDBC
 * driver does not actually expose JDBC 4 compliant {@code SQLException} subclasses.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see SQLTransientException
 * @see SQLTransientException
 * @see SQLRecoverableException
 */
public class SQLExceptionSubclassTranslator extends AbstractFallbackSQLExceptionTranslator {

	public SQLExceptionSubclassTranslator() {
		setFallbackTranslator(new SQLStateSQLExceptionTranslator());
	}

	@Override
	@Nullable
	protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
		if (ex instanceof SQLTransientException) {
			if (ex instanceof SQLTransientConnectionException) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
			}
			else if (ex instanceof SQLTransactionRollbackException) {
				return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
			}
			else if (ex instanceof SQLTimeoutException) {
				return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
			}
		}
		else if (ex instanceof SQLNonTransientException) {
			if (ex instanceof SQLNonTransientConnectionException) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
			}
			else if (ex instanceof SQLDataException) {
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			else if (ex instanceof SQLIntegrityConstraintViolationException) {
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			else if (ex instanceof SQLInvalidAuthorizationSpecException) {
				return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
			}
			else if (ex instanceof SQLSyntaxErrorException) {
				return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
			}
			else if (ex instanceof SQLFeatureNotSupportedException) {
				return new InvalidDataAccessApiUsageException(buildMessage(task, sql, ex), ex);
			}
		}
		else if (ex instanceof SQLRecoverableException) {
			return new RecoverableDataAccessException(buildMessage(task, sql, ex), ex);
		}

		// Fallback to Spring's own SQL state translation...
		return null;
	}

}
