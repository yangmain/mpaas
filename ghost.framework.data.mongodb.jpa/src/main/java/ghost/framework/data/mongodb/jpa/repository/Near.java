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
package ghost.framework.data.mongodb.jpa.repository;

import ghost.framework.data.geo.Distance;
import ghost.framework.data.geo.Point;

import java.lang.annotation.*;

/**
 * Annotation to be used for disambiguing method parameters that shall be used to trigger geo near queries. By default
 * those parameters are found without the need for additional annotation if they are the only parameters of the
 * according type (e.g. {@link Point}, {@code double[]}, {@link Distance}).
 *
 * @author Oliver Gierke
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Near {
}
