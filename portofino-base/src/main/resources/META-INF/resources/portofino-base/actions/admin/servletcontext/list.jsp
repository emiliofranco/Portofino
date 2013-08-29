<%@ page contentType="text/html;charset=UTF-8" language="java"
         pageEncoding="UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"
%><%@taglib prefix="mde" uri="/manydesigns-elements"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.actions.admin.servletcontext.ServletContextAction"/>
<stripes:layout-render name="/portofino-base/admin-theme/admin-page.jsp">
    <stripes:layout-component name="pageTitle">
        <fmt:message key="layouts.admin.modules.title"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletTitle">
        <fmt:message key="layouts.admin.modules.title"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <mde:write name="actionBean" property="form"/>
        <stripes:form beanclass="com.manydesigns.portofino.actions.admin.servletcontext.ServletContextAction"
                      method="post">
            <div class="form-actions">
                <button name="returnToPages" type="submit" class="btn">Return to pages</button>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>