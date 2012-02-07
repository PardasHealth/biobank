package edu.ualberta.med.biobank.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.validator.constraints.NotEmpty;

import edu.ualberta.med.biobank.validator.constraint.NotUsed;
import edu.ualberta.med.biobank.validator.constraint.Unique;
import edu.ualberta.med.biobank.validator.group.PreDelete;
import edu.ualberta.med.biobank.validator.group.PrePersist;

@Entity
@Table(name = "SHIPPING_METHOD")
@Unique(properties = "name", groups = PrePersist.class)
@NotUsed(by = ShipmentInfo.class, property = "shippingMethod", groups = PreDelete.class)
public class ShippingMethod extends AbstractBiobankModel {
    private static final long serialVersionUID = 1L;

    private String name;

    @NotEmpty(message = "{edu.ualberta.med.biobank.model.ShippingMethod.name.NotEmpty}")
    @Column(name = "NAME", unique = true, nullable = false)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}