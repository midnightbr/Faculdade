<%@page import="model.Raca" %>
	<%@page import="java.util.List" %>
		<%@page import="dao.RacaDAO" %>
			<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
				<!DOCTYPE html>
				<html>

				<head>
					<meta charset="UTF-8">
					<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
					<!-- <link href="./css/style.css" rel="stylesheet" type="text/css"> -->
					<title>Cadastrar Raça</title>
				</head>

				<body>
					<div class="form">
						<form action="salvar_canino.jsp" method="post">
							Id: <input type="text" name="txt_id_raca" required>
							Nome: <input type="text" name="txt_nome_raca" required>
							<button type="submit" value="submit">Inserir Raça</button>
						</form>
					</div>
				</body>

				</html>