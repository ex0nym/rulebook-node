package io.exonym.rulebook.schema;

import io.exonym.lite.pojo.TypeNames;
import io.exonym.utils.storage.XContainerSchema;

// TODO: 21.04.21 There's no need for this.
public class XNodeContainerSchema extends XContainerSchema {

	public XNodeContainerSchema() {
		this.setType(TypeNames.CONTAINERS_USERNAME);
		
	}
}
