<%@ page import="model.Raca" %>
<%@ page import="dao.RacaDAO" %>
<%@ page import="java.util.List" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Excluir Raça</title>
</head>
<body>
	<div class="form">
		<form action="deletar_raca.jsp" method="post">
			ID: <input type="text" name="txt_id_raca">
			Raca: <input type="text" name="txt_nome_raca">
			<button type="submit" value="submit">Deletar Raça</button>
		</form>
		<a href="listaracas.jsp">Não sabe o ID da raça?</a>
	</div>

</body>
</html>