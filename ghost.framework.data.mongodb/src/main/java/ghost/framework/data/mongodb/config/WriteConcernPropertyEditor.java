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
package ghost.framework.data.mongodb.config;

import com.mongodb.WriteConcern;
import ghost.framework.beans.annotation.constraints.Nullable;
import ghost.framework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * Parse a string to a {@link WriteConcern}. If it is a well know {@link String} as identified by the
 * {@link WriteConcern#valueOf(String)}, use the well known {@link WriteConcern} value, otherwise pass the string as is
 * to the constructor of the write concern. There is no support for other constructor signatures when parsing from a
 * string value.
 *
 * @author Mark Pollack
 * @author Christoph Strobl
 */
public class WriteConcernPropertyEditor extends PropertyEditorSupport {

	/**
	 * Parse a string to a {@link WriteConcern}.
	 */
	@Override
	public void setAsText(@Nullable String writeConcernString) {

		if (!StringUtils.hasText(writeConcernString)) {
			return;
		}

		WriteConcern writeConcern = WriteConcern.valueOf(writeConcernString);
		if (writeConcern != null) {
			// have a well known string
			setValue(writeConcern);
		} else {
			// pass on the string to the constructor
			setValue(new WriteConcern(writeConcernString));
		}
	}
}
