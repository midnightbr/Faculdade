<%@ page import="model.Raca" %>
<%@ page import="java.util.List" %>
<%@ page import="dao.RacaDAO" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%
	// Carregando todas as estruturas necessarias
	RacaDAO racaDao = new RacaDAO();
	List<Raca> _racas = racaDao.getAll();
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Adicionar novo cachorro</title>
</head>
<body>
	<h2>Cadastrar novo canino</h2>
	<form method="post" action="salvar_canino.jsp">
		<fieldset>
			<legend>Dados do canino</legend>
			<label for="txt_id_cao">ID:</label>
			<input name="txt_id_cao" type="text" value=""> </br>
			
			<label for="txt_nome_cao">Nome:</label>
			<input name="txt_nome_cao" type="text" value=""> </br>
			
			<label for="cmb_sexo_cao">Sexo:</label>
			<select name="sel_sexo_cao">
				<option value="F">Fêmea</option>
				<option value="M">Macho</option>
			</select>
		</fieldset>
		<fieldset>
			<legend>Raça do canino</legend>
			<%
				for (Raca raca : _racas) {%>
					<input name="rad_raca_cao" type="radio" 
						value="<%=raca.getId() %>"/> <%=raca.getNome() %> <br/>
			<%}%>	
		</fieldset> </br>
		<button type="submit" value="Submit">Enviar Dados</button>
	</form>
</body>
</html>