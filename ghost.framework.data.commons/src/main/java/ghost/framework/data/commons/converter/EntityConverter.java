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

import ghost.framework.context.converter.ConverterContainer;
import ghost.framework.data.commons.mapping.PersistentEntity;
import ghost.framework.data.commons.mapping.PersistentProperty;
import ghost.framework.data.commons.mapping.context.MappingContext;

/**
 * Combined {@link EntityReader} and {@link EntityWriter} and add the ability to access a {@link MappingContext} and
 * {@link ConverterContainer}.
 *
 * @param <E> the concrete {@link PersistentEntity} implementation the converter is based on.
 * @param <P> the concrete {@link PersistentProperty} implementation the converter is based on.
 * @param <T> the most common type the {@link EntityConverter} can actually convert.
 * @param <S> the store specific source and sink an {@link EntityConverter} can deal with.
 * @author Oliver Gierke
 */
public interface EntityConverter<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>, T, S> extends EntityReader<T, S>, EntityWriter<T, S> {

	/**
	 * Returns the underlying {@link MappingContext} used by the converter.
	 *
	 * @return never {@literal null}
	 */
	MappingContext<? extends E, P> getMappingContext();

	/**
	 * Returns the underlying {@link ConverterContainer} used by the converter.
	 *
	 * @return never {@literal null}.
	 */
	ConverterContainer getConverterContainer();
}
