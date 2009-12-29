<!DOCTYPE hibernate-configuration PUBLIC
    "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
    "http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd">

<hibernate-configuration>
  <session-factory>
  <property name="hibernate.connection.provider_class">org.opentaps.foundation.entity.hibernate.OpentapsConnectionProvider</property>
  <property name="hibernate.transaction.factory_class">org.opentaps.foundation.entity.hibernate.OpentapsTransactionFactory</property>
  <property name="hibernate.transaction.manager_lookup_class">org.opentaps.foundation.entity.hibernate.OpentapsTransactionManagerLookup</property>
  <property name="hibernate.search.default.directory_provider">org.hibernate.search.store.FSDirectoryProvider</property>
  <property name="hibernate.cache.provider_class">org.hibernate.cache.EhCacheProvider</property>
  <property name="net.sf.ehcache.configurationResourceName">ehcache.xml</property>
  <property name="hibernate.search.worker.execution">sync</property>
  <property name="hibernate.cache.use_query_cache">true</property>
  <property name="hibernate.cache.use_second_level_cache">true</property>
<#list entities as entity>
  <mapping class="org.opentaps.base.entities.${entity}"/>
</#list>
  </session-factory>
</hibernate-configuration>
