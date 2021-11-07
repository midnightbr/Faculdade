<%@ page import="model.Raca" %>
<%@ page import="dao.RacaDAO" %>
<%@ page import="model.Cao" %>
<%@ page import="dao.CaoDAO" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%
	// Aqui carregaremos todas as estruturas de dados necessárias a esta página
	// Também realizaremos as operações (serviços) a que esta página se propõe
	CaoDAO caoDao = new CaoDAO();
	RacaDAO racaDao = new RacaDAO();
	
	// Pegando os parâmetros enviados pelo form:
	int cao_iden = Integer.parseInt(request.getParameter("txt_id_cao"));
	String cao_nome = request.getParameter("txt_nome_cao");
	String cao_sexo = request.getParameter("sel_sexo_cao");
	int cao_raca_iden = Integer.parseInt(request.getParameter("rad_raca_cao"));
	
	// Montando o objeto Raca
	Raca raca = racaDao.getById(cao_raca_iden);
	
	Cao novoCao = new Cao();
	novoCao.setId(cao_iden);
	novoCao.setNome(cao_nome);
	novoCao.setSexo(cao_sexo);
	novoCao.setRaca(raca);
	
	caoDao.add(novoCao);
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Canino Salvo</title>
</head>
<body>
	<h2>Canino Inserido com Sucesso!</h2>
	</br>
	<p>Parabéns, você inseriu um novo canino no banco de dados!</p>
	</br>
	<p>
	<%=novoCao.toString() %>
	</p>
	<a href="listacaes.jsp">Listagem dos caninos</a>
	<a href="novo_canino.jsp">Adicionar novo canino	</a>
</body>
</html>