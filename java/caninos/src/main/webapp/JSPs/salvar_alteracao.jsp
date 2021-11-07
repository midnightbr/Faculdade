<%@ page import="model.Raca" %>
<%@ page import="dao.RacaDAO" %>
<%@ page import="java.util.List" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	RacaDAO racaDao = new RacaDAO();
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Alterar Raça</title>
</head>
<body>
	<%
		String id_raca = request.getParameter("txt_id_raca");
		String nome_raca = request.getParameter("txt_nome_raca");
		out.print(racaDao.alterarRaca(id_raca, nome_raca));
	%>
	<a href="alterar_raca.jsp">Voltar</a>
	<a href="listaracas.jsp">Lista Raça</a>
</body>
</html>