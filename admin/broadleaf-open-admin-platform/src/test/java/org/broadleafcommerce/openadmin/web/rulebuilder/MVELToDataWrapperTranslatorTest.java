/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.openadmin.web.rulebuilder;

import junit.framework.TestCase;
import org.broadleafcommerce.openadmin.client.dto.Entity;
import org.broadleafcommerce.openadmin.client.dto.Property;
import org.broadleafcommerce.openadmin.web.rulebuilder.dto.DataDTO;
import org.broadleafcommerce.openadmin.web.rulebuilder.dto.DataWrapper;
import org.broadleafcommerce.openadmin.web.rulebuilder.dto.ExpressionDTO;
import org.broadleafcommerce.openadmin.web.rulebuilder.service.CustomerFieldServiceImpl;
import org.broadleafcommerce.openadmin.web.rulebuilder.service.OrderFieldServiceImpl;
import org.broadleafcommerce.openadmin.web.rulebuilder.service.OrderItemFieldServiceImpl;

/**
 * @author Elbert Bautista (elbertbautista)
 */
public class MVELToDataWrapperTranslatorTest extends TestCase {

    private OrderItemFieldServiceImpl orderItemFieldService;
    private CustomerFieldServiceImpl customerFieldService;
    private OrderFieldServiceImpl orderFieldService;

    @Override
    protected void setUp() {
        orderItemFieldService = new OrderItemFieldServiceImpl();
        customerFieldService = new CustomerFieldServiceImpl();
        orderFieldService = new OrderFieldServiceImpl();
    }

    /**
     * Tests the creation of a DataWrapper given an mvel/quantity property
     * @throws MVELTranslationException
     */
    public void testCreateRuleData() throws MVELTranslationException {
        MVELToDataWrapperTranslator translator = new MVELToDataWrapperTranslator();

        Property[] properties = new Property[2];
        Property mvelProperty = new Property();
        mvelProperty.setName("orderItemMatchRule");
        mvelProperty.setValue("MVEL.eval(\"toUpperCase()\",discreteOrderItem.category.name)==MVEL.eval(\"toUpperCase()\",\"merchandise\")");
        Property quantityProperty = new Property();
        quantityProperty.setName("quantity");
        quantityProperty.setValue("1");
        properties[0] = mvelProperty;
        properties[1] = quantityProperty;
        Entity[] entities = new Entity[1];
        Entity entity = new Entity();
        entity.setProperties(properties);
        entities[0] = entity;

        DataWrapper dataWrapper = translator.createRuleData(entities, "orderItemMatchRule", "quantity", orderItemFieldService);
        assert(dataWrapper.getData().size() == 1);
        assert(dataWrapper.getData().get(0).getQuantity() == 1);
        assert(dataWrapper.getData().get(0).getGroups().size()==1);
        assert(dataWrapper.getData().get(0).getGroups().get(0) instanceof ExpressionDTO);
        ExpressionDTO exp = (ExpressionDTO) dataWrapper.getData().get(0).getGroups().get(0);
        assert(exp.getName().equals("category.name"));
        assert(exp.getOperator().equals(BLCOperator.IEQUALS.name()));
        assert(exp.getValue().equals("merchandise"));
    }

    public void testCustomerQualificationDataWrapper() throws MVELTranslationException {
        MVELToDataWrapperTranslator translator = new MVELToDataWrapperTranslator();

        Property[] properties = new Property[1];
        Property mvelProperty = new Property();
        mvelProperty.setName("matchRule");
        mvelProperty.setValue("customer.emailAddress!=customer.username&&customer.deactivated==true");
        properties[0] = mvelProperty;
        Entity[] entities = new Entity[1];
        Entity entity = new Entity();
        entity.setProperties(properties);
        entities[0] = entity;

        DataWrapper dataWrapper = translator.createRuleData(entities, "matchRule", null, customerFieldService);
        assert(dataWrapper.getData().size() == 1);
        assert(dataWrapper.getData().get(0).getQuantity() == null);
        assert(dataWrapper.getData().get(0).getGroupOperator().equals(BLCOperator.AND.name()));
        assert(dataWrapper.getData().get(0).getGroups().size()==2);

        assert(dataWrapper.getData().get(0).getGroups().get(0) instanceof ExpressionDTO);
        ExpressionDTO e1 = (ExpressionDTO) dataWrapper.getData().get(0).getGroups().get(0);
        assert(e1.getName().equals("emailAddress"));
        assert(e1.getOperator().equals(BLCOperator.NOT_EQUAL_FIELD.name()));
        assert(e1.getValue().equals("username"));

        assert(dataWrapper.getData().get(0).getGroups().get(1) instanceof ExpressionDTO);
        ExpressionDTO e2 = (ExpressionDTO) dataWrapper.getData().get(0).getGroups().get(1);
        assert(e2.getName().equals("deactivated"));
        assert(e2.getOperator().equals(BLCOperator.EQUALS.name()));
        assert(e2.getValue().equals("true"));

    }

    public void testOrderQualificationDataWrapper() throws MVELTranslationException {
        MVELToDataWrapperTranslator translator = new MVELToDataWrapperTranslator();

        Property[] properties = new Property[1];
        Property mvelProperty = new Property();
        mvelProperty.setName("matchRule");
        mvelProperty.setValue("order.subTotal.getAmount()>=100&&(order.currency.defaultFlag==true||order.locale.localeCode==\"my\")");
        properties[0] = mvelProperty;
        Entity[] entities = new Entity[1];
        Entity entity = new Entity();
        entity.setProperties(properties);
        entities[0] = entity;

        DataWrapper dataWrapper = translator.createRuleData(entities, "matchRule", null, orderFieldService);
        assert(dataWrapper.getData().size() == 1);
        assert(dataWrapper.getData().get(0).getQuantity() == null);
        assert(dataWrapper.getData().get(0).getGroupOperator().equals(BLCOperator.AND.name()));
        assert(dataWrapper.getData().get(0).getGroups().size()==2);

        assert(dataWrapper.getData().get(0).getGroups().get(0) instanceof ExpressionDTO);
        ExpressionDTO e1 = (ExpressionDTO) dataWrapper.getData().get(0).getGroups().get(0);
        assert(e1.getName().equals("subTotal"));
        assert(e1.getOperator().equals(BLCOperator.GREATER_OR_EQUAL.name()));
        assert(e1.getValue().equals("100"));

        assert(dataWrapper.getData().get(0).getGroups().get(1) != null);
        DataDTO d1 = dataWrapper.getData().get(0).getGroups().get(1);
        assert(d1.getGroupOperator().equals(BLCOperator.OR.name()));
        assert(d1.getGroups().get(0) instanceof ExpressionDTO);
        ExpressionDTO d1e1 = (ExpressionDTO) d1.getGroups().get(0);
        assert(d1e1.getName().equals("currency.defaultFlag"));
        assert(d1e1.getOperator().equals(BLCOperator.EQUALS.name()));
        assert(d1e1.getValue().equals("true"));
        assert(d1.getGroups().get(1) instanceof ExpressionDTO);
        ExpressionDTO d1e2 = (ExpressionDTO) d1.getGroups().get(1);
        assert(d1e2.getName().equals("locale.localeCode"));
        assert(d1e2.getOperator().equals(BLCOperator.EQUALS.name()));
        assert(d1e2.getValue().equals("my"));

    }

    public void testItemQualificationDataWrapper() throws MVELTranslationException {
        MVELToDataWrapperTranslator translator = new MVELToDataWrapperTranslator();

        Property[] p1 = new Property[2];
        Property m1 = new Property();
        m1.setName("orderItemMatchRule");
        m1.setValue("discreteOrderItem.category.name==\"test category\"");
        Property q1 = new Property();
        q1.setName("quantity");
        q1.setValue("1");
        p1[0] = m1;
        p1[1] = q1;
        Entity e1 = new Entity();
        e1.setProperties(p1);

        Property[] p2 = new Property[2];
        Property m2 = new Property();
        m2.setName("orderItemMatchRule");
        m2.setValue("!(discreteOrderItem.product.manufacturer==\"test manufacturer\"&&discreteOrderItem.product.model==\"test model\")");
        Property q2 = new Property();
        q2.setName("quantity");
        q2.setValue("2");
        p2[0] = m2;
        p2[1] = q2;
        Entity e2 = new Entity();
        e2.setProperties(p2);

        Entity[] entities = new Entity[2];
        entities[0] = e1;
        entities[1] = e2;

        DataWrapper dataWrapper = translator.createRuleData(entities, "orderItemMatchRule", "quantity", orderItemFieldService);
        assert(dataWrapper.getData().size() == 2);

        assert(dataWrapper.getData().get(0).getQuantity() == 1);
        assert(dataWrapper.getData().get(0).getGroupOperator().equals(BLCOperator.AND.name()));
        assert(dataWrapper.getData().get(0).getGroups().size()==1);
        assert(dataWrapper.getData().get(0).getGroups().get(0) instanceof ExpressionDTO);
        ExpressionDTO exp1 = (ExpressionDTO) dataWrapper.getData().get(0).getGroups().get(0);
        assert(exp1.getName().equals("category.name"));
        assert(exp1.getOperator().equals(BLCOperator.EQUALS.name()));
        assert(exp1.getValue().equals("test category"));

        assert(dataWrapper.getData().get(1).getQuantity() == 2);
        assert(dataWrapper.getData().get(1).getGroupOperator().equals(BLCOperator.NOT.name()));
        assert(dataWrapper.getData().get(1).getGroups().size()==2);

        assert(dataWrapper.getData().get(1).getGroups().get(0) instanceof ExpressionDTO);
        ExpressionDTO expd1e1 = (ExpressionDTO) dataWrapper.getData().get(1).getGroups().get(0);
        assert(expd1e1.getName().equals("product.manufacturer"));
        assert(expd1e1.getOperator().equals(BLCOperator.EQUALS.name()));
        assert(expd1e1.getValue().equals("test manufacturer"));

        assert(dataWrapper.getData().get(1).getGroups().get(1) instanceof ExpressionDTO);
        ExpressionDTO expd1e2 = (ExpressionDTO) dataWrapper.getData().get(1).getGroups().get(1);
        assert(expd1e2.getName().equals("product.model"));
        assert(expd1e2.getOperator().equals(BLCOperator.EQUALS.name()));
        assert(expd1e2.getValue().equals("test model"));
    }
}
