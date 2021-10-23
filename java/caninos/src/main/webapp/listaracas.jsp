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
<title>Lista de Raças</title>
</head>
<body>
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
	
	<a href="cadastrar_caninos.jsp">Adicionar Raça</a>
	<a href="apagar_raca.jsp">Excluir Raça</a>
	<a href="alterar_raca.jsp">Alterar Raça</a>

</body>
</html>