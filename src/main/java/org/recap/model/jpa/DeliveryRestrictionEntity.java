package org.recap.model.jpa;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;

/**
 * Created by harikrishnanv on 3/4/17.
 */

@Entity
@Table(name="delivery_restriction_cross_partner_t",schema="recap",catalog="")
@AttributeOverride(name = "id", column = @Column(name = "DELIVERY_RESTRICTION_CROSS_PARTNER_ID"))
public class DeliveryRestrictionEntity extends AbstractEntity<Integer>  {
    @Column(name="DELIVERY_RESTRICTIONS")
    private String deliveryRestriction;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "INSTITUTION_ID", insertable = false, updatable = false)
    private InstitutionEntity institutionEntity;

    @ManyToMany(mappedBy = "deliveryRestrictionEntityList")
    private List<CustomerCodeEntity> customerCodeEntityList;

    public String getDeliveryRestriction() {
        return deliveryRestriction;
    }

    public void setDeliveryRestriction(String deliveryRestriction) {
        this.deliveryRestriction = deliveryRestriction;
    }

    public InstitutionEntity getInstitutionEntity() {
        return institutionEntity;
    }

    public void setInstitutionEntity(InstitutionEntity institutionEntity) {
        this.institutionEntity = institutionEntity;
    }

    public List<CustomerCodeEntity> getCustomerCodeEntityList() {
        return customerCodeEntityList;
    }

    public void setCustomerCodeEntityList(List<CustomerCodeEntity> customerCodeEntityList) {
        this.customerCodeEntityList = customerCodeEntityList;
    }
}
