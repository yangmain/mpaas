///*
// * Copyright 2002-2012 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package ghost.framework.web.module.handler;
//
//import ghost.framework.web.module.handler.servlet.AbstractUrlHandlerMapping;
//
///**
// * Abstract implementation of the {@link ghost.framework.web.servlet.HandlerMapping}
// * interface, detecting URL mappings for handler beans through introspection of all
// * defined beans in the application context.
// *
// * @author Juergen Hoeller
// * @since 2.5
// * @see #determineUrlsForHandler
// */
//public abstract class AbstractDetectingUrlHandlerMapping extends AbstractUrlHandlerMapping {
//
//	private boolean detectHandlersInAncestorContexts = false;
//
//
//	/**
//	 * Set whether to detect handler beans in ancestor ApplicationContexts.
//	 * <p>Default is "false": Only handler beans in the current ApplicationContext
//	 * will be detected, i.e. only in the context that this HandlerMapping itself
//	 * is defined in (typically the current DispatcherServlet's context).
//	 * <p>Switch this flag on to detect handler beans in ancestor contexts
//	 * (typically the Spring root WebApplicationContext) as well.
//	 */
//	public void setDetectHandlersInAncestorContexts(boolean detectHandlersInAncestorContexts) {
//		this.detectHandlersInAncestorContexts = detectHandlersInAncestorContexts;
//	}
//
//
////	/**
////	 * Calls the {@link #detectHandlers()} method in addition to the
////	 * superclass's initialization.
////	 */
////	@Override
////	public void initApplicationContext() throws ApplicationContextException {
////		super.initApplicationContext();
////		detectHandlers();
////	}
//
//	/**
//	 * Register all handlers found in the current ApplicationContext.
//	 * <p>The actual URL determination for a handler is up to the concrete
//	 * {@link #determineUrlsForHandler(String)} implementation. A bean for
//	 * which no such URLs could be determined is simply not considered a handler.
//	 * @throws ghost.framework.beans.BeanException if the handler couldn't be registered
//	 * @see #determineUrlsForHandler(String)
//	 */
//	protected void detectHandlers() /*throws BeanException */{
////		ApplicationContext applicationContext = obtainApplicationContext();
////		String[] beanNames = (this.detectHandlersInAncestorContexts ?
////				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class) :
////				applicationContext.getBeanNamesForType(Object.class));
//
//		// Take any bean name that we can determine URLs for.
////		for (String beanName : beanNames) {
////			String[] urls = determineUrlsForHandler(beanName);
////			if (!ObjectUtils.isEmpty(urls)) {
////				// URL paths found: Let's consider it a handler.
////				registerHandler(urls, beanName);
////			}
////		}
//
//		if ((logger.isDebugEnabled() && !getHandlerMap().isEmpty()) || logger.isTraceEnabled()) {
//			logger.debug("Detected " + getHandlerMap().size() + " mappings in " + formatMappingName());
//		}
//	}
//
//
//	/**
//	 * Determine the URLs for the given handler bean.
//	 * @param beanName the name of the candidate bean
//	 * @return the URLs determined for the bean, or an empty array if none
//	 */
//	protected abstract String[] determineUrlsForHandler(String beanName);
//
//}
