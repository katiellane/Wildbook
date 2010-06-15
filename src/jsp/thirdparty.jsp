<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
        <%@ page contentType="text/html; charset=utf-8" language="java" import="org.ecocean.*,java.util.Properties, java.io.FileInputStream, java.io.File, java.io.FileNotFoundException" %>
<%

//setup our Properties object to hold all properties
	Properties props=new Properties();
	String langCode="en";
	
	//check what language is requested
	if(request.getParameter("langCode")!=null){
		if(request.getParameter("langCode").equals("fr")) {langCode="fr";}
		if(request.getParameter("langCode").equals("de")) {langCode="de";}
		if(request.getParameter("langCode").equals("es")) {langCode="es";}
	}
	
	//set up the file input stream
	//FileInputStream propsInputStream=new FileInputStream(new File((new File(".")).getCanonicalPath()+"/webapps/ROOT/WEB-INF/classes/bundles/"+langCode+"/submit.properties"));
	//props.load(propsInputStream);
	props.load(getClass().getResourceAsStream("/bundles/"+langCode+"/submit.properties"));

	
	//load our variables for the submit page
	String title=props.getProperty("submit_title");
	String submit_maintext=props.getProperty("submit_maintext");
	String submit_reportit=props.getProperty("reportit");
	String submit_language=props.getProperty("language");
	String what_do=props.getProperty("what_do");
	String read_overview=props.getProperty("read_overview");
	String see_all_encounters=props.getProperty("see_all_encounters");
	String see_all_sharks=props.getProperty("see_all_sharks");
	String report_encounter=props.getProperty("report_encounter");
	String log_in=props.getProperty("log_in");
	String contact_us=props.getProperty("contact_us");
	String search=props.getProperty("search");
	String encounter=props.getProperty("encounter");
	String shark=props.getProperty("shark");
	String join_the_dots=props.getProperty("join_the_dots");
	String menu=props.getProperty("menu");
	String last_sightings=props.getProperty("last_sightings");
	String more=props.getProperty("more");
	String ws_info=props.getProperty("ws_info");
	String about=props.getProperty("about");
	String contributors=props.getProperty("contributors");
	String forum=props.getProperty("forum");
	String blog=props.getProperty("blog");
	String area=props.getProperty("area");
	String match=props.getProperty("match");
	
	//link path to submit page with appropriate language
	String submitPath="submit.jsp?langCode="+langCode;
	
%>

<html>
<head>
<title><%=title%></title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<meta name="Description"
	content="<%=CommonConfiguration.getHTMLDescription() %>" />
<meta name="Keywords"
	content="<%=CommonConfiguration.getHTMLKeywords() %>" />
<meta name="Author" content="<%=CommonConfiguration.getHTMLAuthor() %>" />
<link href="<%=CommonConfiguration.getCSSURLLocation() %>"
	rel="stylesheet" type="text/css" />
<link rel="shortcut icon"
	href="<%=CommonConfiguration.getHTMLShortcutIcon() %>" />

<link rel="shortcut icon" href="images/favicon.ico" />

</head>

<body>
<div id="wrapper">
<div id="page">
<jsp:include page="header.jsp" flush="true">
	<jsp:param name="isResearcher" value="<%=request.isUserInRole("researcher")%>"/>
	<jsp:param name="isManager" value="<%=request.isUserInRole("manager")%>"/>
	<jsp:param name="isReviewer" value="<%=request.isUserInRole("reviewer")%>"/>
	<jsp:param name="isAdmin" value="<%=request.isUserInRole("admin")%>"/>
</jsp:include>	
<div id="main">
	
	<div id="maincol-wide">

		<div id="maintext">
		  <h1 class="intro">Third Party Software</h1>
		</div>
			
		<p>The following third party software is used with permission and/or under license within the Shepherd Project Framework:</p>
		<p><strong>Third Party Commercial Licenses (used with permission and/or under non-commercial guidelines)</strong></p>
		<ul>
		  <li>Dynamic Image as included in this software is used with the permission of its developer Guang Yang and licensed only for use with the Shepherd Project Framework for non-commercial, mark-recapture studies.</li>
	    <li>CSSPlay for dropdown navigation menus is used under non-commercial guidelines. <a href="http://www.cssplay.co.uk/menus/simple_vertical.html">http://www.cssplay.co.uk/menus/simple_vertical.html</a></li>
		</ul>
		<p><strong>Open Source Components </strong></p>
		<ul>
		<li>KeenerView for Flex-based image display is used under the MIT license: <a href="http://code.google.com/p/keenerview/">http://code.google.com/p/keenerview/</a></li>
		<li>DataNucleus Access Platform is used for object-relational mapping and persistence under the Apache 2 License: <a href="http://www.datanucleus.org%20">http://www.datanucleus.org</a></li>
		<li>Many, many more...TBD </li>
		</ul>

	  </div>
	<!-- end maintext -->

  </div><!-- end maincol -->

<jsp:include page="footer.jsp" flush="true" />
</div><!-- end page -->
</div><!--end wrapper -->
</body>
</html>
