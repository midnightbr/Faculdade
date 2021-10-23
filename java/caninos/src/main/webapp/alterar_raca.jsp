<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Alterar Raça</title>
</head>
<body>
	<div class="form">
		<form action="salvar_alteracao.jsp" method="post">
			ID: <input type="text" name="txt_id_raca">
			Raça: <input type="text" name="txt_nome_raca">
			<button type="submit" value="submit">Alterar Raça</button>
		</form>
		<a href="listaracas.jsp">Não sabe o ID da raça?</a>
	</div>

</body>
</html>