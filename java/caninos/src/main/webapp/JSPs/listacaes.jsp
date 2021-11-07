<%@page import="model.Cao" %>
	<%@page import="java.util.List" %>
		<%@page import="dao.CaoDAO" %>
			<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

				<% // Estruturas de dados que serão usadas no decorrer desta página. 
					CaoDAO caoDao = new CaoDAO();
					List<Cao> caes = caoDao.getAll();
					%>
					<!DOCTYPE html>
					<html>

					<head>
						<meta charset="UTF-8">
						<title>Lista dos Cães</title>
					</head>

					<body>
						<table>
							<tr>
								<th>Nome</th>
								<th>Raca</th>
								<th>Sexo</th>
							</tr>

							<% // Aqui abrimos Script de servidor para gerar uma linha <tr> para cada cao da lista

								for (Cao cao : caes) {
								out.print("<tr>");
									out.print("<td>" + cao.getNome() + "</td>");
									out.print("<td>" + cao.getRaca().getNome() + "</td>");
									out.print("<td>" + cao.getSexo() + "</td>");
									}
									// Abaixo fechamos o script de servidor
									%>
						</table>

					</body>

					</html>