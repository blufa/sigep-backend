@org.hibernate.annotations.FilterDef(
        name = "tenantFilter",
        parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = Long.class),
        defaultCondition = "tenant_id = :tenantId"
)
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package gn.departement.sigep.common;
