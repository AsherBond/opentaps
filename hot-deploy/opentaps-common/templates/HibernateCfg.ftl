<!DOCTYPE hibernate-configuration PUBLIC
    "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
    "http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd">

<hibernate-configuration>
  <session-factory>
  <property name="hibernate.connection.provider_class">org.opentaps.foundation.entity.hibernate.OpentapsConnectionProvider</property>  
  <property name="hibernate.transaction.factory_class">org.opentaps.foundation.entity.hibernate.OpentapsTransactionFactory</property>
  <property name="hibernate.transaction.manager_lookup_class">org.opentaps.foundation.entity.hibernate.OpentapsTransactionManagerLookup</property>  
  <property name="hibernate.search.default.directory_provider">org.hibernate.search.store.FSDirectoryProvider</property>
<#list entities as entity>
  <mapping class="org.opentaps.domain.base.entities.${entity}"/>
</#list>
  </session-factory>
</hibernate-configuration>
