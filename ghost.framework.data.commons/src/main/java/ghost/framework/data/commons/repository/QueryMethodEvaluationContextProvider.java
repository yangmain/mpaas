/*
 * Copyright 2014-2020 the original author or authors.
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
package ghost.framework.data.commons.repository;

//import ghost.framework.expression.EvaluationContext;
//import ghost.framework.expression.spel.support.StandardEvaluationContext;

//import ghost.framework.data.jdbc.jpa.plugin.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider;

import ghost.framework.data.commons.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider;
import ghost.framework.data.commons.repository.query.Parameters;
import ghost.framework.expression.EvaluationContext;

import java.util.Collections;

/**
 * Provides a way to access a centrally defined potentially shared {@link StandardEvaluationContext}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.9
 */
public interface QueryMethodEvaluationContextProvider {
	static QueryMethodEvaluationContextProvider DEFAULT = new ExtensionAwareQueryMethodEvaluationContextProvider(
			Collections.emptyList());

	/**
	 * Returns an {@link EvaluationContext} built using the given {@link Parameters} and parameter values.
	 *
	 * @param parameters the {@link Parameters} instance obtained from the query method the context is built for.
	 * @param parameterValues the values for the parameters.
	 * @return
	 */
	<T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues);
}
