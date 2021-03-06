/*
 * Copyright 2002-2018 the original author or authors.
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

package ghost.framework.context.proxy.aop.autoproxy.target;

import ghost.framework.beans.annotation.constraints.Nullable;
import ghost.framework.context.annotation.GenericBeanDefinition;
import ghost.framework.context.bean.DefaultListableBeanFactory;
import ghost.framework.context.bean.IBeanDefinition;
import ghost.framework.context.factory.BeanFactory;
import ghost.framework.context.factory.BeanFactoryAware;
import ghost.framework.context.factory.ConfigurableBeanFactory;
import ghost.framework.context.proxy.aop.AopInfrastructureBean;
import ghost.framework.context.proxy.aop.TargetSource;
import ghost.framework.context.proxy.aop.autoproxy.TargetSourceCreator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

//import ghost.framework.aop.TargetSource;
//import ghost.framework.aop.framework.AopInfrastructureBean;
//import ghost.framework.aop.framework.autoproxy.TargetSourceCreator;
//import ghost.framework.aop.target.AbstractBeanFactoryBasedTargetSource;
//import ghost.framework.beans.factory.BeanFactory;
//import ghost.framework.beans.factory.BeanFactoryAware;
//import ghost.framework.beans.factory.DisposableBean;
//import ghost.framework.beans.factory.config.IBeanDefinition;
//import ghost.framework.beans.factory.config.ConfigurableBeanFactory;
//import ghost.framework.beans.factory.support.DefaultListableBeanFactory;
//import ghost.framework.beans.factory.support.GenericBeanDefinition;
//import ghost.framework.lang.Nullable;

/**
 * Convenient superclass for
 * {@link ghost.framework.aop.framework.autoproxy.TargetSourceCreator}
 * implementations that require creating multiple instances of a prototype bean.
 *
 * <p>Uses an internal BeanFactory to manage the target instances,
 * copying the original bean definition to this internal factory.
 * This is necessary because the original BeanFactory will just
 * contain the proxy instance created through auto-proxying.
 *
 * <p>Requires running in an
 * {@link ghost.framework.beans.factory.support.AbstractBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ghost.framework.aop.target.AbstractBeanFactoryBasedTargetSource
 * @see ghost.framework.beans.factory.support.AbstractBeanFactory
 */
public abstract class AbstractBeanFactoryBasedTargetSourceCreator
		implements TargetSourceCreator, BeanFactoryAware, AutoCloseable {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConfigurableBeanFactory beanFactory;

	/** Internally used DefaultListableBeanFactory instances, keyed by bean name. */
	private final Map<String, DefaultListableBeanFactory> internalBeanFactories =
			new HashMap<>();
	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Cannot do auto-TargetSource creation with a BeanFactory " +
					"that doesn't implement ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	/**
	 * Return the BeanFactory that this TargetSourceCreators runs in.
	 */
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of the TargetSourceCreator interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public final TargetSource getTargetSource(Class<?> beanClass, String beanName) {
		AbstractBeanFactoryBasedTargetSource targetSource = createBeanFactoryBasedTargetSource(beanClass, beanName);
		if (targetSource == null) {
			return null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Configuring AbstractBeanFactoryBasedTargetSource: " + targetSource);
		}
		DefaultListableBeanFactory internalBeanFactory = getInternalBeanFactoryForBean(beanName);
		// We need to override just this bean definition, as it may reference other beans
		// and we're happy to take the parent's definition for those.
		// Always use prototype scope if demanded.
		IBeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);
		GenericBeanDefinition bdCopy = new GenericBeanDefinition(bd);
		if (isPrototypeBased()) {
//			bdCopy.setScope(IBeanDefinition.SCOPE_PROTOTYPE);
		}
//		internalBeanFactory.registerBeanDefinition(beanName, bdCopy);

		// Complete configuring the PrototypeTargetSource.
//		targetSource.setTargetBeanName(beanName);
//		targetSource.setBeanFactory(internalBeanFactory);

		return targetSource;
	}

	/**
	 * Return the internal BeanFactory to be used for the specified bean.
	 * @param beanName the name of the target bean
	 * @return the internal BeanFactory to be used
	 */
	protected DefaultListableBeanFactory getInternalBeanFactoryForBean(String beanName) {
		synchronized (this.internalBeanFactories) {
			DefaultListableBeanFactory internalBeanFactory = this.internalBeanFactories.get(beanName);
			if (internalBeanFactory == null) {
				internalBeanFactory = buildInternalBeanFactory(this.beanFactory);
				this.internalBeanFactories.put(beanName, internalBeanFactory);
			}
			return internalBeanFactory;
		}
	}

	/**
	 * Build an internal BeanFactory for resolving target beans.
	 * @param containingFactory the containing BeanFactory that originally defines the beans
	 * @return an independent internal BeanFactory to hold copies of some target beans
	 */
	protected DefaultListableBeanFactory buildInternalBeanFactory(ConfigurableBeanFactory containingFactory) {
		// Set parent so that references (up container hierarchies) are correctly resolved.
		DefaultListableBeanFactory internalBeanFactory = new DefaultListableBeanFactory(containingFactory);
		// Required so that all BeanPostProcessors, Scopes, etc become available.
		internalBeanFactory.copyConfigurationFrom(containingFactory);

		// Filter out BeanPostProcessors that are part of the AOP infrastructure,
		// since those are only meant to apply to beans defined in the original factory.
		internalBeanFactory.getBeanPostProcessors().removeIf(beanPostProcessor ->
				beanPostProcessor instanceof AopInfrastructureBean);

		return internalBeanFactory;
	}

	@Override
	public void close() throws Exception {
		synchronized (this.internalBeanFactories) {
			for (DefaultListableBeanFactory bf : this.internalBeanFactories.values()) {
				bf.destroySingletons();
			}
		}
	}

//	/**
//	 * Destroys the internal BeanFactory on shutdown of the TargetSourceCreator.
//	 * @see #getInternalBeanFactoryForBean
//	 */
//	@Override
//	public void destroy() {
//		synchronized (this.internalBeanFactories) {
//			for (DefaultListableBeanFactory bf : this.internalBeanFactories.values()) {
//				bf.destroySingletons();
//			}
//		}
//	}


	//---------------------------------------------------------------------
	// Template methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Return whether this TargetSourceCreator is prototype-based.
	 * The scope of the target bean definition will be set accordingly.
	 * <p>Default is "true".
	 * @see ghost.framework.beans.factory.config.IBeanDefinition#isSingleton()
	 */
	protected boolean isPrototypeBased() {
		return true;
	}

	/**
	 * Subclasses must implement this method to return a new AbstractPrototypeBasedTargetSource
	 * if they wish to create a custom TargetSource for this bean, or {@code null} if they are
	 * not interested it in, in which case no special target source will be created.
	 * Subclasses should not call {@code setTargetBeanName} or {@code setBeanFactory}
	 * on the AbstractPrototypeBasedTargetSource: This class' implementation of
	 * {@code getTargetSource()} will do that.
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return the AbstractPrototypeBasedTargetSource, or {@code null} if we don't match this
	 */
	@Nullable
	protected abstract AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName);

}
