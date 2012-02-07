package edu.ualberta.med.biobank.common.peer;

import edu.ualberta.med.biobank.common.util.TypeReference;
import java.util.Collections;
import edu.ualberta.med.biobank.common.wrappers.Property;
import java.util.List;
import java.util.ArrayList;
import edu.ualberta.med.biobank.model.Clinic;
import edu.ualberta.med.biobank.model.Study;
import java.util.Collection;
import edu.ualberta.med.biobank.model.Contact;

public class ContactPeer {
	public static final Property<Integer, Contact> ID = Property.create(
		"id" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<Integer>() {}
		, new Property.Accessor<Integer, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public Integer get(Contact model) {
				return model.getId();
			}
			@Override
			public void set(Contact model, Integer value) {
				model.setId(value);
			}
		});

	public static final Property<String, Contact> TITLE = Property.create(
		"title" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Contact model) {
				return model.getTitle();
			}
			@Override
			public void set(Contact model, String value) {
				model.setTitle(value);
			}
		});

	public static final Property<String, Contact> FAX_NUMBER = Property.create(
		"faxNumber" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Contact model) {
				return model.getFaxNumber();
			}
			@Override
			public void set(Contact model, String value) {
				model.setFaxNumber(value);
			}
		});

	public static final Property<String, Contact> NAME = Property.create(
		"name" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Contact model) {
				return model.getName();
			}
			@Override
			public void set(Contact model, String value) {
				model.setName(value);
			}
		});

	public static final Property<String, Contact> OFFICE_NUMBER = Property.create(
		"officeNumber" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Contact model) {
				return model.getOfficeNumber();
			}
			@Override
			public void set(Contact model, String value) {
				model.setOfficeNumber(value);
			}
		});

	public static final Property<String, Contact> PAGER_NUMBER = Property.create(
		"pagerNumber" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Contact model) {
				return model.getPagerNumber();
			}
			@Override
			public void set(Contact model, String value) {
				model.setPagerNumber(value);
			}
		});

	public static final Property<String, Contact> EMAIL_ADDRESS = Property.create(
		"emailAddress" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Contact model) {
				return model.getEmailAddress();
			}
			@Override
			public void set(Contact model, String value) {
				model.setEmailAddress(value);
			}
		});

	public static final Property<String, Contact> MOBILE_NUMBER = Property.create(
		"mobileNumber" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Contact model) {
				return model.getMobileNumber();
			}
			@Override
			public void set(Contact model, String value) {
				model.setMobileNumber(value);
			}
		});

	public static final Property<Clinic, Contact> CLINIC = Property.create(
		"clinic" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<Clinic>() {}
		, new Property.Accessor<Clinic, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public Clinic get(Contact model) {
				return model.getClinic();
			}
			@Override
			public void set(Contact model, Clinic value) {
				model.setClinic(value);
			}
		});

	public static final Property<Collection<Study>, Contact> STUDY_COLLECTION = Property.create(
		"studyCollection" //$NON-NLS-1$
		, Contact.class
		, new TypeReference<Collection<Study>>() {}
		, new Property.Accessor<Collection<Study>, Contact>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<Study> get(Contact model) {
				return model.getStudyCollection();
			}
			@Override
			public void set(Contact model, Collection<Study> value) {
				model.getStudyCollection().clear();
				model.getStudyCollection().addAll(value);
			}
		});

   public static final List<Property<?, ? super Contact>> PROPERTIES;
   static {
      List<Property<?, ? super Contact>> aList = new ArrayList<Property<?, ? super Contact>>();
      aList.add(ID);
      aList.add(TITLE);
      aList.add(FAX_NUMBER);
      aList.add(NAME);
      aList.add(OFFICE_NUMBER);
      aList.add(PAGER_NUMBER);
      aList.add(EMAIL_ADDRESS);
      aList.add(MOBILE_NUMBER);
      aList.add(CLINIC);
      aList.add(STUDY_COLLECTION);
      PROPERTIES = Collections.unmodifiableList(aList);
   };
}