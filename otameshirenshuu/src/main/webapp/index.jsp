<%@ page contentType="text/html; charset=UTF-8" %>
<%-- ルートアクセスは新しいMVC版の入口(/denpyo)へ転送する --%>
<% response.sendRedirect(request.getContextPath() + "/denpyo"); %>
