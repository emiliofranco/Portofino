/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */
package com.manydesigns.portofino.context.hibernate;


import com.manydesigns.portofino.database.ConnectionProvider;
import com.manydesigns.portofino.database.DbUtil;
import com.manydesigns.portofino.database.JdbcConnectionProvider;
import com.manydesigns.portofino.database.Type;
import com.manydesigns.portofino.model.datamodel.Database;
import com.manydesigns.portofino.model.datamodel.Reference;
import com.manydesigns.portofino.model.datamodel.Relationship;
import com.manydesigns.portofino.model.datamodel.Schema;
import org.hibernate.FetchMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.*;

import java.util.*;
import java.util.List;


/**
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Angelo    Lupo       - angelo.lupo@manydesigns.com
 * @author Paolo     Predonzani - paolo.predonzani@manydesigns.com
 */
public class HibernateConfig {

    protected final ConnectionProvider connectionProvider;


    public HibernateConfig(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Configuration buildSessionFactory(Database database) {
        try {
            Configuration configuration = new Configuration()
                    .setProperty("default_entity_mode", "dynamic-map");

            JdbcConnectionProvider jdbcConnectionProvider =
                    (JdbcConnectionProvider) connectionProvider;
            configuration.setProperty("hibernate.connection.url",
                    jdbcConnectionProvider.getConnectionURL())
                    .setProperty("hibernate.connection.driver_class",
                            jdbcConnectionProvider.getDriverClass())
                    .setProperty("hibernate.connection.username",
                            jdbcConnectionProvider.getUsername())
                    .setProperty("hibernate.connection.password",
                            jdbcConnectionProvider.getPassword())
                    .setProperty("hibernate.current_session_context_class",
                            "org.hibernate.context.ThreadLocalSessionContext");
            configuration.setProperty("hibernate.show_sql", "true");


            Mappings mappings = configuration.createMappings();
            for (Schema schema : database.getSchemas()) {
                for (com.manydesigns.portofino.model.datamodel.Table aTable :
                        schema.getTables()) {
                    RootClass clazz = createTableMapping(
                            mappings, aTable);
                    mappings.addClass(clazz);
                    mappings.addImport(clazz.getEntityName(),
                            aTable.getTableName());
                    mappings.addImport(clazz.getEntityName(),
                            clazz.getEntityName());
                }
            }



            for (Schema schema : database.getSchemas()) {
                for (com.manydesigns.portofino.model.datamodel.Table aTable :
                        schema.getTables()) {
                    for (Relationship rel : aTable.getOneToManyRelationships()) {
                        createO2M(configuration, mappings, rel);
                    }
                }
            }

            for (Schema schema : database.getSchemas()) {
                for (com.manydesigns.portofino.model.datamodel.Table aTable :
                        schema.getTables()) {
                    for (Relationship rel : aTable.getManyToOneRelationships()) {
                        if (aTable.isM2m()) {
                            System.out.println(aTable+" "+rel.getRelationshipName());
                            createM2O(configuration, mappings, rel);
                        }
                    }
                }
            }

            return configuration;
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    protected RootClass createTableMapping(Mappings mappings,
                                           com.manydesigns.portofino.model.datamodel.Table aTable) {


        Table tab = mappings.addTable(aTable.getSchemaName(), null,
                aTable.getTableName(), null, false);
        tab.setName(aTable.getTableName());
        tab.setSchema(aTable.getSchemaName());
        mappings.addTableBinding(aTable.getSchemaName(), null,
                aTable.getTableName(), aTable.getTableName(), null);

        RootClass clazz = null;


        clazz = new RootClass();
        clazz.setEntityName(aTable.getQualifiedName());
        if (aTable.getClassName() != null) {
            clazz.setClassName(aTable.getClassName());
            clazz.setProxyInterfaceName(aTable.getClassName());
        }
        clazz.setLazy(true);
        clazz.setTable(tab);
        clazz.setNodeName(aTable.getTableName());


        List<com.manydesigns.portofino.model.datamodel.Column> columnList =
                new ArrayList<com.manydesigns.portofino.model.datamodel.Column>();
        columnList.addAll(aTable.getColumns());

        //Primary keys
        List<com.manydesigns.portofino.model.datamodel.Column> columnPKList
                = aTable.getPrimaryKey().getColumns();

        if (columnPKList.size() > 1) {
            createPKComposite(mappings, aTable, aTable.getPrimaryKey().getPkName(),
                    clazz, tab, columnPKList);
        } else {
            createPKSingle(mappings, aTable, aTable.getPrimaryKey().getPkName(),
                    clazz, tab, columnPKList);
        }

        //Other columns
        columnList.removeAll(columnPKList);

        for (com.manydesigns.portofino.model.datamodel.Column column
                : columnList) {
            createColumn(mappings, clazz, tab, column);

        }

        return clazz;
    }

    protected void createColumn(Mappings mappings, RootClass clazz,
                                Table tab,
                                com.manydesigns.portofino.model.datamodel.Column column) {
        Column col = new Column();
        col.setName(column.getColumnName());
        col.setLength(column.getLength());
        col.setPrecision(column.getLength());
        col.setScale(column.getScale());
        col.setNullable(column.isNullable());
        String columnType = column.getColumnType();
        Type type = connectionProvider.getTypeByName(columnType);
        col.setSqlTypeCode(type.getJdbcType());
        tab.addColumn(col);

        Property prop = new Property();
        if (column.getPropertyName() != null) {
            prop.setName(column.getPropertyName());
            prop.setNodeName(column.getPropertyName());
        } else {
            prop.setName(column.getColumnName());
            prop.setNodeName(column.getColumnName());
        }
        SimpleValue value = new SimpleValue();
        value.setTable(tab);
        org.hibernate.type.Type hibernateType =
                DbUtil.getHibernateType(type.getJdbcType());
        value.setTypeName(hibernateType.getName());
        value.addColumn(col);
        clazz.addProperty(prop);
        prop.setValue(value);

        mappings.addColumnBinding(column.getColumnName(),
                col, tab);
    }

    protected void createPKComposite(Mappings mappings,
                                     com.manydesigns.portofino.model.datamodel.Table mdTable,
                                     String pkName, RootClass clazz,
                                     Table tab,
                                     List<com.manydesigns.portofino.model.datamodel.Column> columnPKList) {


        final PrimaryKey primaryKey = new PrimaryKey();
        primaryKey.setName(pkName);
        primaryKey.setTable(tab);
        tab.setPrimaryKey(primaryKey);

        clazz.setEmbeddedIdentifier(true);
        Component component = new Component(clazz);
        component.setDynamic(mdTable.getClassName()==null);
        String name;
        name = mdTable.getQualifiedName();

        component.setRoleName(name + ".id");
        component.setEmbedded(true);
        component.setNodeName("id");
        component.setKey(true);
        component.setNullValue("undefined");

        if (!component.isDynamic()){
            component.setComponentClassName
                    (mdTable.getClassName());
            /*component.setEmbedded(true);
            clazz.setMutable(false);
            clazz.setEmbeddedIdentifier(true);*/
        }

        for (com.manydesigns.portofino.model.datamodel.Column
                column : columnPKList) {
            Column col = new Column();
            col.setName(column.getColumnName());
            String columnType = column.getColumnType();

            Type type = connectionProvider.getTypeByName(columnType);
            col.setSqlTypeCode(type.getJdbcType());
            primaryKey.addColumn(col);
            SimpleValue value = new SimpleValue();
            value.setTable(tab);
            value.setTypeName(DbUtil.getHibernateType(type.getJdbcType())
                    .getName());
            value.addColumn(col);


            tab.getPrimaryKey().addColumn(col);
            tab.addColumn(col);
            Property prop = new Property();
            prop.setName(column.getColumnName());
            prop.setValue(value);
            prop.setCascade("none");
            prop.setNodeName(column.getColumnName());
            prop.setPropertyAccessorName("property");

            //Provo ad aggiungere proprietà alla classe
            //prop.setInsertable(false);
            //prop.setUpdateable(false);
            //clazz.addProperty(prop);
            
            component.addProperty(prop);
            mappings.addColumnBinding(column.getColumnName(),
                    col, tab);
        }
        tab.setIdentifierValue(component);
        clazz.setIdentifier(component);
        /*Property id = new Property();
        id.setName("id");
        id.setNodeName("id");
        id.setPropertyAccessorName("property");
        id.setValue(component);
        clazz.setIdentifierProperty(id);*/
        clazz.setDiscriminatorValue(name);

    }


    protected void createPKSingle(Mappings mappings,
                                  com.manydesigns.portofino.model.datamodel.Table mdTable,
                                  String pkName, RootClass clazz,
                                  Table tab,
                                  List<com.manydesigns.portofino.model.datamodel.Column> columnPKList) {
        com.manydesigns.portofino.model.datamodel.Column
                column = columnPKList.get(0);
        SimpleValue id = new SimpleValue(tab);
        final PrimaryKey primaryKey = new PrimaryKey();
        primaryKey.setName(pkName);
        primaryKey.setTable(tab);
        tab.setPrimaryKey(primaryKey);

        id.setTypeName(column.getColumnType());
        Column col = new Column();
        col.setName(column.getColumnName());
        String columnType = column.getColumnType();
        col.setValue(id);
        col.setLength(column.getLength());
        col.setPrecision(column.getLength());
        col.setScale(column.getScale());
        col.setNullable(column.isNullable());
        Type type = connectionProvider.getTypeByName(columnType);
        col.setSqlTypeCode(type.getJdbcType());
        org.hibernate.type.Type hibernateType = DbUtil.getHibernateType(type.getJdbcType());
        id.setTypeName(hibernateType.getName());

        mappings.addColumnBinding(column.getColumnName(),
                col, tab);

        tab.addColumn(col);
        tab.getPrimaryKey().addColumn(col);
        id.addColumn(col);

        Property prop = new Property();
        if (column.getPropertyName() != null) {
            prop.setName(column.getPropertyName());
            prop.setNodeName(column.getPropertyName());
        } else {
            prop.setName(column.getColumnName());
            prop.setNodeName(column.getColumnName());
        }
        prop.setValue(id);
        prop.setPropertyAccessorName(mappings.getDefaultAccess());
        PropertyGeneration generation = PropertyGeneration.parse(null);
        prop.setGeneration(generation);

        prop.setInsertable(false);
        prop.setUpdateable(false);
        clazz.addProperty(prop);


        if (type.isAutoincrement()) {
            id.setIdentifierGeneratorStrategy("identity");
            Properties params = new Properties();
            params.put(PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
                    mappings.getObjectNameNormalizer());

            params.setProperty(
                    PersistentIdentifierGenerator.SCHEMA,
                    tab.getSchema());
            id.setIdentifierGeneratorProperties(params);
            id.setNullValue(null);
        }

        tab.setIdentifierValue(id);

        clazz.setIdentifier(id);
        clazz.setIdentifierProperty(prop);
        clazz.setDiscriminatorValue(mdTable.getQualifiedName());

    }


    protected void createO2M(
            Configuration config,
            Mappings mappings,
            Relationship relationship) {
        com.manydesigns.portofino.model.datamodel.Table manyTable
                = relationship.getFromTable();
        com.manydesigns.portofino.model.datamodel.Table oneTable
                = relationship.getToTable();


        PersistentClass clazzOne;
        PersistentClass clazzMany;

        clazzOne = config.getClassMapping
                (oneTable.getQualifiedName());

        clazzMany = config.getClassMapping
                (manyTable.getQualifiedName());


        Bag set = new Bag(clazzOne);
        set.setLazy(true);
        //Mettere Lazy in debug a false per ottenere subito eventuali errori
        if (relationship.getManyPropertyName() == null) {
            set.setRole(manyTable.getQualifiedName() + "."
                    + relationship.getRelationshipName());
            set.setNodeName(relationship.getRelationshipName());
        } else {
            set.setRole(relationship.getManyPropertyName());
            set.setNodeName(relationship.getManyPropertyName());
        }
        set.setCollectionTable(clazzMany.getTable());
        OneToMany oneToMany = new OneToMany(set.getOwner());
        set.setElement(oneToMany);

        oneToMany.setReferencedEntityName(manyTable.getQualifiedName());

        oneToMany.setAssociatedClass(clazzMany);
        oneToMany.setEmbedded(true);

        set.setSorted(false);
        set.setFetchMode(FetchMode.DEFAULT);
        //Riferimenti alle colonne

        DependantValue dv;
        Table tableMany = clazzMany.getTable();
        Table tableOne = clazzOne.getTable();
        List<Column> oneColumns = new ArrayList<Column>();
        List<Column> manyColumns = new ArrayList<Column>();
        //Chiave multipla
        final List<Reference> refs = relationship.getReferences();
        if (refs.size() > 1) {

            Component component = new Component(set);
            component.setDynamic(manyTable.getClassName()==null);
            component.setEmbedded(true);
            dv = new DependantValue(clazzMany.getTable(), component);
            dv.setNullable(true);
            dv.setUpdateable(true);


            for (Reference ref : relationship.getReferences()) {
                String colToName = ref.getToColumn().getColumnName();
                String colFromName = ref.getFromColumn().getColumnName();
                Iterator it = tableMany.getColumnIterator();
                while (it.hasNext()) {
                    Column col = (Column) it.next();
                    if (col.getName().equals(colFromName)) {
                        dv.addColumn(col);
                        manyColumns.add(col);
                        break;
                    }
                }

                Iterator it2 = tableOne.getColumnIterator();
                while (it2.hasNext()) {
                    Column col = (Column) it.next();
                    if (col.getName().equals(colToName)) {
                        oneColumns.add(col);
                        break;
                    }
                }
                Property refProp;
                refProp = getRefProperty(clazzOne, colToName);
                component.addProperty(refProp);
            }


        } else {  //chiave straniera singola
            Property refProp;

            String colFromName = refs.get(0).getFromColumn().getColumnName();
            String colToName = refs.get(0).getToColumn().getColumnName();
            refProp = getRefProperty(clazzOne, colToName);
            dv = new DependantValue(clazzMany.getTable(),
                    refProp.getPersistentClass().getKey());
            dv.setNullable(true);
            dv.setUpdateable(true);

            Iterator it = clazzMany.getTable().getColumnIterator();
            while (it.hasNext()) {
                Column col = (Column) it.next();
                if (col.getName().equals(colFromName)) {
                    dv.addColumn(col);
                    manyColumns.add(col);
                    break;
                }
            }

            Iterator it2 = tableOne.getColumnIterator();
            while (it2.hasNext()) {
                Column col = (Column) it2.next();
                if (col.getName().equals(colToName)) {
                    oneColumns.add(col);
                    break;
                }
            }
        }

        tableMany.createForeignKey(relationship.getRelationshipName(),
                manyColumns,
                oneTable.getQualifiedName(),
                oneColumns);
        set.setKey(dv);
        mappings.addCollection(set);

        Property prop = new Property();
        if (relationship.getManyPropertyName() == null) {
            prop.setName(manyTable.getQualifiedName() + "."
                    + relationship.getRelationshipName());
            prop.setNodeName(relationship.getRelationshipName());
        } else {
            prop.setName(relationship.getManyPropertyName());
            prop.setNodeName(relationship.getManyPropertyName());
        }
        prop.setValue(set);
        clazzOne.addProperty(prop);
    }

    private Property getRefProperty(PersistentClass clazzOne, String colToName) {
        Property refProp;
        if (null != clazzOne.getIdentifierProperty()) {
            refProp = clazzOne.getIdentifierProperty();
        } else if (null != clazzOne.getIdentifier()) {
            refProp = ((Component) clazzOne.getIdentifier())
                    .getProperty(colToName);
        } else {
            refProp = clazzOne.getProperty(colToName);
        }
        return refProp;
    }

    protected void createM2O(Configuration config, Mappings mappings,
                             Relationship relationship) {
        RootClass clazz = (RootClass) mappings.getClass(
                relationship.getFromTable().getQualifiedName());
        Table tab = clazz.getTable();
        List<com.manydesigns.portofino.model.datamodel.Column> cols =
                new ArrayList<com.manydesigns.portofino.model.datamodel.Column>();

        for (Reference ref : relationship.getReferences()) {
            cols.add(ref.getFromColumn());
        }

        ManyToOne m2o = new ManyToOne(tab);
        m2o.setLazy(false);
        final HashMap<String, PersistentClass> persistentClasses =
                new HashMap<String, PersistentClass>();
        persistentClasses.put(relationship.getToTable().getQualifiedName(),
                config.getClassMapping(relationship.getToTable().getQualifiedName()));
        m2o.setReferencedEntityName(relationship.getToTable().getQualifiedName());
        m2o.createPropertyRefConstraints(persistentClasses);
        for (com.manydesigns.portofino.model.datamodel.Column column : cols) {
            Column col = new Column();
            col.setName(column.getColumnName());
            m2o.addColumn(col);
        }

        Property prop = new Property();
        prop.setName(relationship.getOnePropertyName());
        prop.setNodeName(relationship.getOnePropertyName());
        prop.setValue(m2o);
        prop.setInsertable(false);
        prop.setUpdateable(false);
        clazz.addProperty(prop);

    }
}