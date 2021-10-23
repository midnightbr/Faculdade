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
<title>Excluir Raça</title>
</head>
<body>
	<%
		int id_raca = 0;
		String raca_id = request.getParameter("txt_id_raca");
		// int id_raca = Integer.parseInt(request.getParameter("txt_id_raca"));
		String nome_raca = request.getParameter("txt_nome_raca");
		if (raca_id != "") {
			id_raca = Integer.parseInt(raca_id);			
		}
		if (nome_raca == "") {
			nome_raca = "vazio";
		}
		out.print(racaDao.deletarRaca(id_raca, nome_raca));
	%>

</body>
</html>