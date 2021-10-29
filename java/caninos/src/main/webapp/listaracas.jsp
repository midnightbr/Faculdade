<%@page import="model.Raca" %>
<%@page import="java.util.List" %>
<%@page import="dao.RacaDAO" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
RacaDAO racaDao = new RacaDAO();
List<Raca> racas = racaDao.getAll();
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<title>Lista de Raças</title>
</head>
<body>
	<div class="lista">
		<legend>Lista de Raças</legend>	
		<table>
			<tr>
				<th>ID</th>
				<th>Raça</th>
			</tr>
				
			<%
				for(Raca raca : racas) {
					out.print("<tr>");
					out.print("<td>" + raca.getId() + "</td>");
					out.print("<td>" + raca.getNome() + "</td>");
				}
			%>
		</table>	
	</div>
	
	<a href="cadastrar_raca.jsp">Adicionar Raça</a>
	<a href="apagar_raca.jsp">Excluir Raça</a>
	<a href="alterar_raca.jsp">Alterar Raça</a>

</body>
</html>