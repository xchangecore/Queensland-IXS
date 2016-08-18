package com.saic.uicds.core.infrastructure.marshaller;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.xmlbeans.XmlBeansMarshaller;
import org.springframework.oxm.xmlbeans.XmlBeansValidationFailureException;

public class ValidateAndLogOnlyMarshaller extends XmlBeansMarshaller {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public void validate(XmlObject arg0)
	throws XmlBeansValidationFailureException {
		if (arg0 != null) {
			StringBuffer sb = new StringBuffer();
			sb.append("VALIDATING|");
			if (arg0.schemaType() != null) {
				if (arg0.schemaType().getDocumentElementName() != null &&
						arg0.schemaType().getDocumentElementName().getLocalPart() != null) {
					sb.append("DocElement|");
					sb.append(arg0.schemaType().getDocumentElementName().getLocalPart());
					sb.append("|");
				}
			}
			try {
				super.validate(arg0);
				sb.append("VALID");
				log.info(sb.toString());
			} catch (Exception e) {
				sb.append("INVALID");
				log.info(sb.toString());
			}
		}
		else {
			log.error("UicdsMarshaller:validate passed a null object");
		}
	}
	
}
