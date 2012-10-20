package edu.ualberta.med.biobank.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.NotEmpty;

import edu.ualberta.med.biobank.model.study.Patient;
import edu.ualberta.med.biobank.model.study.Specimen;
import edu.ualberta.med.biobank.model.study.SpecimenGroup;
import edu.ualberta.med.biobank.validator.constraint.NotUsed;
import edu.ualberta.med.biobank.validator.constraint.Unique;
import edu.ualberta.med.biobank.validator.group.PreDelete;
import edu.ualberta.med.biobank.validator.group.PrePersist;

/**
 * A standardised set of regions in a {@link Patient} <em>where</em> a
 * {@link Specimen} was collected from. Potential examples include: colon, ear,
 * leg, kidney, etc.
 * 
 * @author Jonathan Ferland
 */
@Audited
@Entity
@Table(name = "ANOTOMICAL_SOURCE")
@Unique(properties = "name", groups = PrePersist.class)
@NotUsed.List({
    @NotUsed(by = SpecimenGroup.class, property = "anatomicalSource", groups = PreDelete.class)
})
public class AnatomicalSource
    extends VersionedLongIdModel
    implements HasName, HasDescription {
    private static final long serialVersionUID = 1L;

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 5000;

    private String name;
    private String description;

    /**
     * @return a <em>unique</em> name, across all persisted instances of the
     *         descendant type.
     */
    @Override
    @NotEmpty(message = "{AnatomicalSource.name.NotEmpty}")
    @Size(max = MAX_NAME_LENGTH, message = "{AnatomicalSource.name.Size}")
    @Column(name = "NAME", nullable = false, unique = true, length = MAX_NAME_LENGTH)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Size(max = MAX_DESCRIPTION_LENGTH, message = "{AnatomicalSource.description.Size}")
    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", length = MAX_DESCRIPTION_LENGTH)
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }
}
