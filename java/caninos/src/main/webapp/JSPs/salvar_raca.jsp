<%@ page import="model.Raca" %>
<%@ page import="dao.RacaDAO" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	RacaDAO racaDao = new RacaDAO();
	// racaDao.inserirRaca();
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Cadastro de Raça</title>
</head>
<body>
	<%
		String id_raca = request.getParameter("txt_id_raca");
		String nome_raca = request.getParameter("txt_nome_raca");
		
		//out.print(racaDao.inserirRaca(id_raca, nome_raca));
		//racaDao.inserirRaca(id_raca, nome_raca);
		
	%>
</body>
</html>