/*
 * Copyright 2002-2012 the original author or authors.
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

package ghost.framework.context.xml;

import ghost.framework.context.bean.BeanDefinitionHolder;
import org.w3c.dom.Node;

/**
 * Interface used by the {@link DefaultBeanDefinitionDocumentReader}
 * to handle custom, nested (directly under a {@code <bean>}) tags.
 *
 * <p>Decoration may also occur based on custom attributes applied to the
 * {@code <bean>} tag. Implementations are free to turn the metadata in the
 * custom tag into as many
 * {@link ghost.framework.beans.factory.config.IBeanDefinition BeanDefinitions} as
 * required and to transform the
 * {@link ghost.framework.beans.factory.config.IBeanDefinition} of the enclosing
 * {@code <bean>} tag, potentially even returning a completely different
 * {@link ghost.framework.beans.factory.config.IBeanDefinition} to replace the
 * original.
 *
 * <p>{@link BeanDefinitionDecorator BeanDefinitionDecorators} should be aware that
 * they may be part of a chain. In particular, a {@link BeanDefinitionDecorator} should
 * be aware that a previous {@link BeanDefinitionDecorator} may have replaced the
 * original {@link ghost.framework.beans.factory.config.IBeanDefinition} with a
 * {@link ghost.framework.aop.framework.ProxyFactoryBean} definition allowing for
 * custom {@link org.aopalliance.intercept.MethodInterceptor interceptors} to be added.
 *
 * <p>{@link BeanDefinitionDecorator BeanDefinitionDecorators} that wish to add an
 * interceptor to the enclosing bean should extend
 * {@link ghost.framework.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator}
 * which handles the chaining ensuring that only one proxy is created and that it
 * contains all interceptors from the chain.
 *
 * <p>The parser locates a {@link BeanDefinitionDecorator} from the
 * {@link NamespaceHandler} for the namespace in which the custom tag resides.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NamespaceHandler
 * @see BeanDefinitionParser
 */
public interface BeanDefinitionDecorator {

	/**
	 * Parse the specified {@link Node} (either an element or an attribute) and decorate
	 * the supplied {@link ghost.framework.beans.factory.config.IBeanDefinition},
	 * returning the decorated definition.
	 * <p>Implementations may choose to return a completely new definition, which will
	 * replace the original definition in the resulting
	 * {@link ghost.framework.beans.factory.BeanFactory}.
	 * <p>The supplied {@link ParserContext} can be used to register any additional
	 * beans needed to support the main definition.
	 */
	BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext);
}
