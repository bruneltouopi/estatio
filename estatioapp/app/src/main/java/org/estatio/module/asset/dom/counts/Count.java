package org.estatio.module.asset.dom.counts;

import java.math.BigInteger;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Version;
import javax.jdo.annotations.VersionStrategy;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.incode.module.base.dom.utils.TitleBuilder;

import org.estatio.module.asset.dom.Property;
import org.estatio.module.base.dom.UdoDomainObject2;

import lombok.Getter;
import lombok.Setter;

@PersistenceCapable(
        identityType = IdentityType.DATASTORE,
        schema = "dbo"
)
@DatastoreIdentity(
        strategy = IdGeneratorStrategy.IDENTITY,
        column = "id")
@Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@Queries({
        @Query(
                name = "findUnique", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.module.asset.dom.counts.Count "
                        + "WHERE property == :property && date == :date "),
        @Query(
                name = "findByProperty", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.module.asset.dom.counts.Count "
                        + "WHERE property == :property ORDER BY date DESC")
})
@Unique(name = "Count_property_date_UNQ", members = { "property", "date" })
@DomainObject(
        editing = Editing.DISABLED
)
@DomainObjectLayout(
        bookmarking = BookmarkPolicy.AS_ROOT
)
public class Count extends UdoDomainObject2<Count> {

    public Count() {
        super("property, date");
    }

    public Count(
            final Property property,
            final LocalDate date,
            final BigInteger pedestrianCount,
            final BigInteger carCount) {
        this();
        this.property = property;
        this.date = date;
        this.pedestrianCount = pedestrianCount;
        this.carCount = carCount;
    }

    public String title() {
        return TitleBuilder.start()
                .withParent(getProperty())
                .withName(getDate())
                .toString();
    }

    @Column(allowsNull = "false", name = "propertyId")
    @Getter @Setter
    private Property property;

    @Column(allowsNull = "false")
    @Getter @Setter
    private LocalDate date;

    @Column(allowsNull = "true")
    @Getter @Setter
    private BigInteger pedestrianCount;

    @Column(allowsNull = "true")
    @Getter @Setter
    private BigInteger carCount;

    @Override
    public ApplicationTenancy getApplicationTenancy() {
        return getProperty().getApplicationTenancy();
    }

}
