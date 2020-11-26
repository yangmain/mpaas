///*
// * Copyright 2002-2017 the original author or authors.
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
//package ghost.framework.data.jdbc.config;
//
//import ghost.framework.data.jdbc.datasource.init.DataSourceInitializer;
//import org.w3c.dom.Element;
//
///**
// * {@link ghost.framework.beans.factory.xml.BeanDefinitionParser} that parses an {@code initialize-database}
// * element and creates a {@link IBeanDefinition} of type {@link DataSourceInitializer}. Picks up nested
// * {@code script} elements and configures a {@link ResourceDatabasePopulator} for them.
// *
// * @author Dave Syer
// * @author Juergen Hoeller
// * @since 3.0
// */
//class InitializeDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {
//
//	@Override
//	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
//		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DataSourceInitializer.class);
//		builder.addPropertyReference("dataSource", element.getAttribute("data-source"));
//		builder.addPropertyValue("enabled", element.getAttribute("enabled"));
//		DatabasePopulatorConfigUtils.setDatabasePopulator(element, builder);
//		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
//		return builder.getBeanDefinition();
//	}
//
//	@Override
//	protected boolean shouldGenerateId() {
//		return true;
//	}
//
//}